package com.cashpro.payment_service.Service;

import com.cashpro.payment_service.DTO.CreatePaymentRequest;
import com.cashpro.payment_service.DTO.Payload;
import com.cashpro.payment_service.DTO.PaymentResponse;
import com.cashpro.payment_service.Entity.OutboxEvent;
import com.cashpro.payment_service.Entity.Payment;
import com.cashpro.payment_service.Entity.PaymentStatus;
import com.cashpro.payment_service.Exceptions.DuplicatePaymentException;
import com.cashpro.payment_service.Repo.OutboxEventRepository;
import com.cashpro.payment_service.Repo.PaymentRepository;
import jakarta.transaction.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StringRedisTemplate redisTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final Logger log= LoggerFactory.getLogger(PaymentService.class);
    private final MeterRegistry meterRegistry;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest data, String idemKey) {
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

            Payment payment = Payment.builder()
                    .clientId(data.clientId())
                    .debitAccount(data.debitAccount())
                    .creditAccount(data.creditAccount())
                    .amount(data.amount())
                    .currency(data.currency())
                    .status(PaymentStatus.RECEIVED)
                    .idempotencyKey(idemKey)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            Payload payload = Payload.builder()
                    .paymentId(savedPayment.getPaymentId())
                    .clientId(savedPayment.getClientId())
                    .amount(savedPayment.getAmount())
                    .currency(savedPayment.getCurrency())
                    .status(savedPayment.getStatus().name())
                    .build();
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(savedPayment.getPaymentId())
                    .eventType("PAYMENT_RECEIVED")
                    .payload(payload)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(event);
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
