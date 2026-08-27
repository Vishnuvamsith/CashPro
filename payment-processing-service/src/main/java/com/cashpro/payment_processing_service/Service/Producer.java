package com.cashpro.payment_processing_service.Service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Producer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Logger log= LoggerFactory.getLogger(Producer.class);
    public void publish(String paymentId,String Payload)
    {
        log.info("sending updated status of payment {} with payload {}", paymentId, Payload);
        kafkaTemplate.send("payment-processed",paymentId,Payload);
    }
}
