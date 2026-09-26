package com.cashpro.payment_processing_service.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.cashpro.events.PaymentProcessed;

class ProducerTest {

    @Test
    void publishShouldSendPaymentProcessedEvent() {
        KafkaTemplate<String, PaymentProcessed> kafkaTemplate = mock(KafkaTemplate.class);
        Producer producer = new Producer(kafkaTemplate);

        UUID paymentId = UUID.randomUUID();
        PaymentProcessed payload = PaymentProcessed.newBuilder()
                .setPaymentId(paymentId)
                .setStatus("PROCESSED")
                .build();

        SendResult<String, PaymentProcessed> result = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("payment-processed", 0),
                0L,
                0,
                0L,
                0,
                0
        );
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send("payment-processed", paymentId.toString(), payload))
                .thenReturn(CompletableFuture.completedFuture(result));

        producer.publish(paymentId.toString(), payload);

        verify(kafkaTemplate).send("payment-processed", paymentId.toString(), payload);
    }
}
