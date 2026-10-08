package com.cashpro.payment_processing_service.Service;

import com.cashpro.events.PaymentReceived;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface MessageInterface {
    public abstract void validate(ConsumerRecord<String, PaymentReceived> message);
    //public abstract void processMessage(PaymentReceived message);

}
