package com.cashpro.payment_service.Kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaTestProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    public void sendTestMessage()
    {
        kafkaTemplate.send("payment-events","test-001","hello from cashpro");
    }
}
