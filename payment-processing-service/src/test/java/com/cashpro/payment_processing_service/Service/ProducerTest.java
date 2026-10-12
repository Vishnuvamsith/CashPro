package com.cashpro.payment_processing_service.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.cashpro.events.PaymentProcessed;
import com.cashpro.payment_processing_service.Entity.PaymentProcessedModel;
import com.cashpro.payment_processing_service.Repository.payloadRepo;

class ProducerTest {

    @Test
    void publishShouldSendProcessingRowsAndMarkThemPublished() {
        // --- mocks ---
        payloadRepo repo = mock(payloadRepo.class);

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        @SuppressWarnings("unchecked")
        KafkaTemplate<String, PaymentProcessed> kafkaTemplate = mock(KafkaTemplate.class);

        // --- fixture: one PROCESSING row in the DB ---
        UUID paymentId = UUID.randomUUID();
        PaymentProcessedModel row = PaymentProcessedModel.builder()
                .payment_id(paymentId)
                .client_id("client-1")
                .amount(new BigDecimal("12.34"))
                .currency("USD")
                .status("PROCESSING")
                .build();

        when(repo.findAll()).thenReturn(List.of(row));

        // --- stub the Kafka send to complete successfully ---
        SendResult<String, PaymentProcessed> result = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("payment-processed", 0),
                0L, 0, 0L, 0, 0
        );
        when(result.getRecordMetadata()).thenReturn(metadata);

        when(kafkaTemplate.send(eq("payment-processed"), eq(paymentId.toString()), any(PaymentProcessed.class)))
                .thenReturn(CompletableFuture.completedFuture(result));

        // --- SUT: 3-arg constructor (repo, redis, kafka) ---
        Producer producer = new Producer(repo, redisTemplate, kafkaTemplate);

        // --- act ---
        producer.publish();

        // --- verify: sent to Kafka with right topic, key, and payload ---
        // NOTE: PaymentProcessed.getPaymentId() returns java.util.UUID  -> compare UUID to UUID
        //       PaymentProcessed.getStatus()    returns java.lang.String -> compare String to String
        verify(kafkaTemplate).send(
                eq("payment-processed"),
                eq(paymentId.toString()),
                argThat((PaymentProcessed p) ->
                        paymentId.equals(p.getPaymentId())
                                && "PROCESSED".equals(p.getStatus())
                )
        );

        // --- verify: DB row flipped to PUBLISHED and saved ---
        verify(repo).save(argThat(saved -> "PUBLISHED".equals(saved.getStatus())));

        // --- verify: Redis dedup marker written ---
        verify(valueOps).set("payment-processed:" + paymentId, "Processed");
    }
}