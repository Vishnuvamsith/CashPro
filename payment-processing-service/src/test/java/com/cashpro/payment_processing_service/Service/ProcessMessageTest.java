package com.cashpro.payment_processing_service.Service;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cashpro.events.PaymentReceived;
import com.cashpro.payment_processing_service.Entity.PaymentProcessedModel;
import com.cashpro.payment_processing_service.Repository.payloadRepo;

class ProcessMessageTest {

    @Test
    void listenShouldSaveProcessingRow() {
        Producer producer = mock(Producer.class);
        payloadRepo repo = mock(payloadRepo.class);

        // repo.save(...) must return a non-null object for the log line to work
        when(repo.save(org.mockito.ArgumentMatchers.any(PaymentProcessedModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcessMessage processMessage = new ProcessMessage(producer, repo);

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
    }
}