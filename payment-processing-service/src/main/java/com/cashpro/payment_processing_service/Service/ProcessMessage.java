package com.cashpro.payment_processing_service.Service;

import com.cashpro.payment_processing_service.Entity.PaymentProcessedModel;
import com.cashpro.payment_processing_service.Exceptions.ValidationException;
import com.cashpro.payment_processing_service.Repository.payloadRepo;
import jakarta.transaction.Transactional;
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
public class ProcessMessage implements MessageInterface {
    private final Logger log= LoggerFactory.getLogger(ProcessMessage.class);
    private final Producer producer;
    private final payloadRepo repo;
    @KafkaListener(topics = "payment-events")
    @Transactional
    public void listen(ConsumerRecord<String, PaymentReceived> message) {
        log.info("Receive message {}",message.offset());
        log.info("Receive message {}",message.headers());
        log.info("Receive message {}",message.value());
        log.info("Receive message {}",message.key());
        log.info("Receive message {}",message.topic());
        try {
            validate(message);
            PaymentProcessedModel data=PaymentProcessedModel.builder()
                    .payment_id(message.value().getPaymentId())
                    .client_id(message.value().getClientId())
                    .amount(message.value().getAmount())
                    .currency(message.value().getCurrency())
                    .status("PROCESSING")
                    .build();
            PaymentProcessedModel savedPayment= repo.save(data);
            log.info("Saved payment to DB: {}", savedPayment);
//            PaymentProcessed payload = PaymentProcessed.newBuilder()
//                    .setPaymentId(message.value().getPaymentId())
//                    .setStatus("PROCESSED")
//                    .build();
//            producer.publish(message.key(), payload);
//            log.info("Queued PAYMENT_PROCESSED for: {}", message.key());
            // build + publish
        } catch (ValidationException e) {
            log.error("Invalid message at offset {}: {}", message.offset(), e.getMessage());
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
