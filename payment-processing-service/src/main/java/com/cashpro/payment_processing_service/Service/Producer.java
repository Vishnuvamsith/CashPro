package com.cashpro.payment_processing_service.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cashpro.events.PaymentProcessed;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Producer {
    private final KafkaTemplate<String, PaymentProcessed> kafkaTemplate;
    private final Logger log= LoggerFactory.getLogger(Producer.class);
    public void publish(String paymentId, PaymentProcessed payload)
    {
        log.info("sending updated status of payment {} with payload {}", paymentId, payload);
        kafkaTemplate.send("payment-processed", paymentId, payload)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish payment-processed for {}", paymentId, exception);
                        return;
                    }

                    log.info("Published payment-processed for {} to partition {} at offset {}",
                            paymentId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
