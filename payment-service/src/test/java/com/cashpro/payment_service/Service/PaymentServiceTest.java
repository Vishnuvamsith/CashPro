package com.cashpro.payment_service.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.cashpro.payment_service.DTO.CreatePaymentRequest;
import com.cashpro.payment_service.DTO.PaymentResponse;
import com.cashpro.payment_service.Configuration.ApprovalPolicyProperties;
import com.cashpro.payment_service.Entity.OutboxEvent;
import com.cashpro.payment_service.Entity.Payment;
import com.cashpro.payment_service.Entity.PaymentStatus;
import com.cashpro.payment_service.Exceptions.DuplicatePaymentException;
import com.cashpro.payment_service.Repo.OutboxEventRepository;
import com.cashpro.payment_service.Repo.PaymentRepository;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OutboxEventRepository outboxEventRepository;
        private ApprovalPolicyProperties approvalPolicy;
    private PaymentService paymentService;
        private MeterRegistry meterRegistry;

    @BeforeEach
        @SuppressWarnings("unchecked")
        void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        outboxEventRepository = mock(OutboxEventRepository.class);
        approvalPolicy = new ApprovalPolicyProperties();
        approvalPolicy.getThresholds().put("USD", new BigDecimal("1000.00"));
        meterRegistry = new SimpleMeterRegistry();
        paymentService = new PaymentService(paymentRepository, redisTemplate, outboxEventRepository,
                approvalPolicy, meterRegistry);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createPaymentShouldPersistPaymentAndOutboxEvent() {
        when(valueOperations.setIfAbsent(eq("idem:test-key"), eq("PROCESSING"), eq(Duration.ofHours(24))))
                .thenReturn(true);

        Payment savedPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .clientId("client-1")
                .debitAccount("debit-1")
                .creditAccount("credit-1")
                .amount(new BigDecimal("150.50"))
                .currency("USD")
                .status(PaymentStatus.RECEIVED)
                .idempotencyKey("test-key")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentRequest request = new CreatePaymentRequest(
                "client-1",
                "debit-1",
                "credit-1",
                new BigDecimal("150.50"),
                "USD"
        );

        PaymentResponse response = paymentService.createPayment(request, "test-key", "maker-1");

        assertThat(response.paymentId()).isEqualTo(savedPayment.getPaymentId());
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(response.status()).isEqualTo(PaymentStatus.RECEIVED);
        assertThat(meterRegistry.get("cashpro.payment.requests").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("cashpro.payment.outcomes").tag("outcome", "success").counter().count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.get("cashpro.payment.request.duration").timer().count()).isEqualTo(1L);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getIdempotencyKey()).isEqualTo("test-key");

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("PAYMENT_RECEIVED");
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void createPaymentShouldRejectDuplicateIdempotencyKey() {
        when(valueOperations.setIfAbsent(eq("idem:duplicate-key"), eq("PROCESSING"), eq(Duration.ofHours(24))))
                .thenReturn(false);

        CreatePaymentRequest request = new CreatePaymentRequest(
                "client-1",
                "debit-1",
                "credit-1",
                new BigDecimal("25.00"),
                "USD"
        );

        assertThatThrownBy(() -> paymentService.createPayment(request, "duplicate-key", "maker-1"))
                .isInstanceOf(DuplicatePaymentException.class)
                .hasMessage("Cannot accept duplicate payment");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        assertThat(meterRegistry.get("cashpro.payment.idempotency.duplicates").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("cashpro.payment.outcomes").tag("outcome", "duplicate").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void getShouldResolveExistingPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .clientId("client-2")
                .debitAccount("dr-1")
                .creditAccount("cr-1")
                .amount(new BigDecimal("99.99"))
                .currency("EUR")
                .status(PaymentStatus.APPROVED)
                .idempotencyKey("key-2")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.get(paymentId);

        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("99.99"));
    }
}
