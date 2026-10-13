package com.cashpro.payment_processing_service.Service;

import com.cashpro.payment_processing_service.Repository.payloadRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cashpro.events.PaymentProcessed;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class Producer {
    private final payloadRepo repo;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, PaymentProcessed> kafkaTemplate;
    private final Logger log= LoggerFactory.getLogger(Producer.class);
    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)
    public void publish()
    {
        log.info("looking for transactions that haven't been published");
        try
        {
            repo.findAll().stream()
                    .filter(event->event.getStatus().equals("PROCESSING"))
                    .forEach(event->{
                        PaymentProcessed payload = PaymentProcessed.newBuilder()
                                .setPaymentId(event.getPayment_id())
                                .setStatus("PROCESSED")
                                .build();
                        kafkaTemplate.send("payment-processed", event.getPayment_id().toString(), payload)
                                .whenComplete((result, exception) -> {
                                    if (exception == null) {
                                        event.setStatus("PUBLISHED");
                                        repo.save(event);
                                        redisTemplate.opsForValue().set("payment-processed:" + event.getPayment_id(), "Processed");
                                        log.info("Published payment-processed for: {}", event.getPayment_id());
                                    } else {
                                        log.error("Failed to publish payment-processed for: {}", event.getPayment_id(), exception);
                                    }
                                });
                    });
        }
        catch (Exception e)
        {
            log.error("Error while publishing payment-processed events", e);
        }
    }
}
