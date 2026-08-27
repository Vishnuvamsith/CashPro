package com.cashpro.payment_service.Service;

import com.cashpro.payment_service.DTO.CreatePaymentRequest;
import com.cashpro.payment_service.DTO.PaymentResponse;
import com.cashpro.payment_service.Entity.OutboxEvent;
import com.cashpro.payment_service.Entity.Payment;
import com.cashpro.payment_service.Entity.PaymentStatus;
import com.cashpro.payment_service.Exceptions.DBexception;
import com.cashpro.payment_service.Exceptions.DuplicatePaymentException;
import com.cashpro.payment_service.Repo.OutboxEventRepository;
import com.cashpro.payment_service.Repo.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.ILoggerFactory;
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
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest data, String idemKey) {

        String redisKey = "idem:" + idemKey;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(
                        redisKey,
                        "PROCESSING",
                        Duration.ofHours(24)
                );
        log.warn("acquired status:{}", acquired);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new DuplicatePaymentException(
                    "Cannot accept duplicate payment"
            );
        }

        try {
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
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(savedPayment.getPaymentId())
                    .eventType("PAYMENT_RECEIVED")
                    .payload("""
                {
                  "paymentId": "%s",
                  "clientId": "%s",
                  "amount": %s,
                  "currency": "%s",
                  "status": "%s"
                }
                """.formatted(
                            savedPayment.getPaymentId(),
                            savedPayment.getClientId(),
                            savedPayment.getAmount(),
                            savedPayment.getCurrency(),
                            savedPayment.getStatus()
                    ))
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(event);

            return new PaymentResponse(
                    savedPayment.getPaymentId(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getStatus(),
                    savedPayment.getCreatedAt()
            );

        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
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
