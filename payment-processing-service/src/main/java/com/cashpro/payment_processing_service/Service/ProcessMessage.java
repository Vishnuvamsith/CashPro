package com.cashpro.payment_processing_service.Service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.cashpro.events.PaymentProcessed;
import com.cashpro.events.PaymentReceived;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessMessage {
    private final Logger log= LoggerFactory.getLogger(ProcessMessage.class);
    private final Producer producer;
    @KafkaListener(topics = "payment-events")
    public void listen(ConsumerRecord<String, PaymentReceived> message) {
        log.info("Receive message {}",message.offset());
        log.info("Receive message {}",message.headers());
        log.info("Receive message {}",message.value());
        log.info("Receive message {}",message.key());
        log.info("Receive message {}",message.topic());
        PaymentProcessed payload = PaymentProcessed.newBuilder()
            .setPaymentId(message.value().getPaymentId())
            .setStatus("PROCESSED")
            .build();
//        throw new RuntimeException(
//                "Simulated payment-processing failure"
        //);
        producer.publish(message.key(), payload);
        log.info("Queued PAYMENT_PROCESSED for: {}", message.key());
    }
}
