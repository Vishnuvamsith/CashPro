package com.cashpro.payment_processing_service.Service;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cashpro.events.PaymentReceived;
import com.cashpro.payment_processing_service.Entity.PaymentProcessedModel;
import com.cashpro.payment_processing_service.Exceptions.ValidationException;
import com.cashpro.payment_processing_service.Repository.payloadRepo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ProcessMessageTest {

    @Test
    void listenShouldSaveProcessingRow() {
        payloadRepo repo = mock(payloadRepo.class);

        // repo.save(...) must return a non-null object for the log line to work
        when(repo.save(org.mockito.ArgumentMatchers.any(PaymentProcessedModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeterRegistry registry = new SimpleMeterRegistry();
        ProcessMessage processMessage = new ProcessMessage(repo, registry);

        UUID paymentId = UUID.randomUUID();
        PaymentReceived paymentReceived = PaymentReceived.newBuilder()
                .setPaymentId(paymentId)
                .setClientId("client-1")
                .setAmount(new BigDecimal("12.34"))
                .setCurrency("USD")
                .setStatus("RECEIVED")
                .build();

        ConsumerRecord<String, PaymentReceived> record = new ConsumerRecord<>(
                "payment-events",
                0,
                1L,
                "key-1",
                paymentReceived
        );

        processMessage.listen(record);

        verify(repo).save(argThat(saved ->
                saved.getPayment_id().equals(paymentId)
                        && "PROCESSING".equals(saved.getStatus())
                        && "client-1".equals(saved.getClient_id())
                        && new BigDecimal("12.34").compareTo(saved.getAmount()) == 0
                        && "USD".equals(saved.getCurrency())
        ));
        assertThat(registry.get("cashpro.kafka.messages").tag("outcome", "success").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("cashpro.kafka.consumer.processing").timer().count()).isEqualTo(1L);
    }

        @Test
        void listenShouldCountAndRethrowInvalidMessage() {
                MeterRegistry registry = new SimpleMeterRegistry();
                ProcessMessage processMessage = new ProcessMessage(mock(payloadRepo.class), registry);
                ConsumerRecord<String, PaymentReceived> record = new ConsumerRecord<>("payment-events", 0, 2L, "key", null);

                assertThatThrownBy(() -> processMessage.listen(record)).isInstanceOf(ValidationException.class);

                assertThat(registry.get("cashpro.kafka.messages").tag("outcome", "failure").counter().count())
                                .isEqualTo(1.0);
                assertThat(registry.get("cashpro.kafka.consumer.processing").timer().count()).isEqualTo(1L);
        }

    @Test
    void listenShouldIgnorePaymentAwaitingApproval() {
        payloadRepo repo = mock(payloadRepo.class);
        MeterRegistry registry = new SimpleMeterRegistry();
        ProcessMessage processMessage = new ProcessMessage(repo, registry);
        PaymentReceived pending = PaymentReceived.newBuilder()
                .setPaymentId(UUID.randomUUID())
                .setClientId("client-1")
                .setAmount(new BigDecimal("12.34"))
                .setCurrency("USD")
                .setStatus("PENDING_APPROVAL")
                .build();

        processMessage.listen(new ConsumerRecord<>("payment-events", 0, 3L, "key", pending));

        verify(repo, never()).save(org.mockito.ArgumentMatchers.any(PaymentProcessedModel.class));
        assertThat(registry.get("cashpro.kafka.messages").tag("outcome", "ignored").counter().count())
                .isEqualTo(1.0);
    }

        @Test
        void listenShouldIgnoreDuplicatePaymentDelivery() {
                payloadRepo repo = mock(payloadRepo.class);
                UUID paymentId = UUID.randomUUID();
                org.mockito.Mockito.when(repo.existsById(paymentId)).thenReturn(true);
                MeterRegistry registry = new SimpleMeterRegistry();
                ProcessMessage processMessage = new ProcessMessage(repo, registry);
                PaymentReceived received = PaymentReceived.newBuilder()
                                .setPaymentId(paymentId)
                                .setClientId("client-1")
                                .setAmount(new BigDecimal("12.34"))
                                .setCurrency("USD")
                                .setStatus("RECEIVED")
                                .build();

                processMessage.listen(new ConsumerRecord<>("payment-events", 0, 4L, "key", received));

                verify(repo, never()).save(org.mockito.ArgumentMatchers.any(PaymentProcessedModel.class));
                assertThat(registry.get("cashpro.kafka.messages").tag("outcome", "duplicate").counter().count())
                                .isEqualTo(1.0);
        }
}