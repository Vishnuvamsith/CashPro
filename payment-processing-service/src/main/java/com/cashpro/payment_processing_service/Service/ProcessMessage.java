package com.cashpro.payment_processing_service.Service;

import com.cashpro.payment_processing_service.Entity.PaymentProcessedModel;
import com.cashpro.payment_processing_service.Exceptions.ValidationException;
import com.cashpro.payment_processing_service.Repository.payloadRepo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.cashpro.events.PaymentReceived;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessMessage implements MessageInterface {
    private final Logger log= LoggerFactory.getLogger(ProcessMessage.class);
    private final payloadRepo repo;
    private final MeterRegistry meterRegistry;
    @KafkaListener(topics = "payment-events")
    public void listen(ConsumerRecord<String, PaymentReceived> message) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Receive message {}", message.offset());
            log.info("Receive message {}", message.headers());
            log.info("Receive message {}", message.value());
            log.info("Receive message {}", message.key());
            log.info("Receive message {}", message.topic());
            validate(message);
            String paymentStatus = message.value().getStatus();
            if (!"RECEIVED".equals(paymentStatus) && !"APPROVED".equals(paymentStatus)) {
                meterRegistry.counter("cashpro.kafka.messages", "outcome", "ignored").increment();
                log.warn("Ignoring non-processable payment event with status {}", paymentStatus);
                return;
            }
            if (repo.existsById(message.value().getPaymentId())) {
                meterRegistry.counter("cashpro.kafka.messages", "outcome", "duplicate").increment();
                log.info("Ignoring duplicate payment event {}", message.value().getPaymentId());
                return;
            }
            PaymentProcessedModel data = PaymentProcessedModel.builder()
                    .payment_id(message.value().getPaymentId())
                    .client_id(message.value().getClientId())
                    .amount(message.value().getAmount())
                    .currency(message.value().getCurrency())
                    .status("PROCESSING")
                    .build();
            PaymentProcessedModel savedPayment = repo.save(data);
            log.info("Saved payment to DB: {}", savedPayment);
            meterRegistry.counter("cashpro.kafka.messages", "outcome", "success").increment();
        } catch (RuntimeException e) {
            meterRegistry.counter("cashpro.kafka.messages", "outcome", "failure").increment();
            log.error("Payment event processing failed", e);
            throw e;
        } finally {
            sample.stop(Timer.builder("cashpro.kafka.consumer.processing")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
    @Override
    public void validate(ConsumerRecord<String, PaymentReceived> message) {
        if (message == null || message.value() == null)
            throw new ValidationException("Message or value is null");
        PaymentReceived v = message.value();
        if (v.getPaymentId() == null)  throw new ValidationException("paymentId missing");
        if (v.getClientId() == null)   throw new ValidationException("clientId missing");
        if (v.getAmount() == null)     throw new ValidationException("amount missing");
        if (v.getCurrency() == null)   throw new ValidationException("currency missing");
        if (v.getStatus() == null)     throw new ValidationException("status missing");
    }
}
