package com.cashpro.payment_processing_service.Service;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cashpro.events.PaymentReceived;

class ProcessMessageTest {

    @Test
    void listenShouldPublishProcessedEvent() {
        Producer producer = mock(Producer.class);
        ProcessMessage processMessage = new ProcessMessage(producer);

        UUID paymentId = UUID.randomUUID();
        PaymentReceived paymentReceived = PaymentReceived.newBuilder()
                .setPaymentId(paymentId)
                .setClientId("client-1")
                .setAmount(new BigDecimal("12.34"))
                .setCurrency("USD")
                .setStatus("RECEIVED")
                .setTemp("temp")
                .build();

        ConsumerRecord<String, PaymentReceived> record = new ConsumerRecord<>(
                "payment-events",
                0,
                1L,
                "key-1",
                paymentReceived
        );

        processMessage.listen(record);

        verify(producer).publish(
                eq("key-1"),
                argThat(payload ->
                        payload.getPaymentId().equals(paymentId)
                                && "PROCESSED".equals(payload.getStatus())
                )
        );
    }
}
