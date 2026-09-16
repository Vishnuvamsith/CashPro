package com.cashpro.payment_service.Kafka;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cashpro.payment_service.DTO.Payload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaTestProducer {
    private final KafkaTemplate<String, Payload> kafkaTemplate;
    public void sendTestMessage()
    {
        Payload payload = Payload.builder()
                .paymentId(UUID.randomUUID())
                .clientId("test-client")
                .amount(BigDecimal.ONE)
                .currency("USD")
                .status("RECEIVED")
                .build();
        kafkaTemplate.send("payment-events", payload.paymentId().toString(), payload);
    }
}
