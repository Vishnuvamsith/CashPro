package com.cashpro.payment_processing_service.Service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessMessage {
    private final Logger log= LoggerFactory.getLogger(ProcessMessage.class);
    private final Producer producer;
    @KafkaListener(topics = "payment-events", groupId = "payment-processing-service")
    public void listen(ConsumerRecord<String,String> message) {
        log.info("Receive message {}",message.offset());
        log.info("Receive message {}",message.headers());
        log.info("Receive message {}",message.value());
        log.info("Receive message {}",message.key());
        log.info("Receive message {}",message.topic());
        String payload= """
                {
                    "paymentId": "%s",
                    "status": "PROCESSED"
                }
                """.formatted(message.key());
        throw new RuntimeException(
                "Simulated payment-processing failure"
        );
//        producer.publish(message.key(),payload);
//        log.info("Published PAYMENT_PROCESSED for: {}", message.key());
    }
}
