package com.cashpro.payment_service.Service;

import com.cashpro.payment_service.DTO.CreatePaymentRequest;
import com.cashpro.payment_service.DTO.ApprovalRequestPayload;
import com.cashpro.payment_service.DTO.Payload;
import com.cashpro.payment_service.DTO.PaymentResponse;
import com.cashpro.payment_service.Configuration.ApprovalPolicyProperties;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.cashpro.payment_service.Entity.OutboxEvent;
import com.cashpro.payment_service.Entity.Payment;
import com.cashpro.payment_service.Entity.PaymentStatus;
import com.cashpro.payment_service.Exceptions.DuplicatePaymentException;
import com.cashpro.payment_service.Repo.OutboxEventRepository;
import com.cashpro.payment_service.Repo.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StringRedisTemplate redisTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ApprovalPolicyProperties approvalPolicy;
    private final Logger log= LoggerFactory.getLogger(PaymentService.class);
    private final MeterRegistry meterRegistry;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest data, String idemKey, String makerId) {
        String redisKey = "idem:" + idemKey;
        Timer.Sample sample = Timer.start(meterRegistry);
        meterRegistry.counter("cashpro.payment.requests").increment();

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "PROCESSING", Duration.ofHours(24));
            log.warn("acquired status:{}", acquired);

            if (!Boolean.TRUE.equals(acquired)) {
                meterRegistry.counter("cashpro.payment.idempotency.duplicates").increment();
                meterRegistry.counter("cashpro.payment.outcomes", "outcome", "duplicate").increment();
                throw new DuplicatePaymentException("Cannot accept duplicate payment");
            }

            if (makerId == null || makerId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maker identity is required");
            }

            PaymentStatus initialStatus = approvalPolicy.requiresApproval(data.amount(), data.currency())
                    ? PaymentStatus.PENDING_APPROVAL
                    : PaymentStatus.RECEIVED;
            Payment payment = Payment.builder()
                    .clientId(data.clientId())
                    .debitAccount(data.debitAccount())
                    .creditAccount(data.creditAccount())
                    .amount(data.amount())
                    .currency(data.currency())
                    .status(initialStatus)
                    .idempotencyKey(idemKey)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            if (initialStatus == PaymentStatus.RECEIVED) {
                enqueuePaymentReceived(savedPayment);
            } else {
                enqueueApprovalRequested(savedPayment, makerId.trim());
            }
            meterRegistry.counter("cashpro.payment.outcomes", "outcome", "success").increment();
            return new PaymentResponse(
                    savedPayment.getPaymentId(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getStatus(),
                    savedPayment.getCreatedAt()
            );
        } catch (DuplicatePaymentException e) {
            throw e;
        } catch (RuntimeException e) {
            meterRegistry.counter("cashpro.payment.outcomes", "outcome", "failure").increment();
            redisTemplate.delete(redisKey);
            throw e;
        } finally {
            sample.stop(Timer.builder("cashpro.payment.request.duration")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private void enqueuePaymentReceived(Payment payment) {
        Payload payload = Payload.builder()
                .paymentId(payment.getPaymentId())
                .clientId(payment.getClientId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .build();
        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(payment.getPaymentId())
                .eventType("PAYMENT_RECEIVED")
                .payload(toJson(payload))
                .status("PENDING")
                .build());
    }

    private void enqueueApprovalRequested(Payment payment, String makerId) {
        ApprovalRequestPayload payload = new ApprovalRequestPayload(
                payment.getPaymentId(), makerId, payment.getClientId(),
                payment.getAmount(), payment.getCurrency());
        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("PAYMENT")
                .aggregateId(payment.getPaymentId())
                .eventType("APPROVAL_REQUESTED")
                .payload(toJson(payload))
                .status("PENDING")
                .build());
    }

    private ObjectNode toJson(Payload payload) {
        return JsonNodeFactory.instance.objectNode()
                .put("paymentId", payload.paymentId().toString())
                .put("clientId", payload.clientId())
                .put("amount", payload.amount().toPlainString())
                .put("currency", payload.currency())
                .put("status", payload.status());
    }

    private ObjectNode toJson(ApprovalRequestPayload payload) {
        return JsonNodeFactory.instance.objectNode()
                .put("paymentId", payload.paymentId().toString())
                .put("makerId", payload.makerId())
                .put("clientId", payload.clientId())
                .put("amount", payload.amount().toPlainString())
                .put("currency", payload.currency());
    }
    public PaymentResponse get(UUID id) {
        Payment p = paymentRepository.findById(id).orElseThrow();

        return new PaymentResponse(
                p.getPaymentId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }
}
