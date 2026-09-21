//package com.cashpro.payment_service.Kafka;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//import org.apache.avro.Conversions.DecimalConversion;
//
//import com.cashpro.events.PaymentReceived;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class KafkaTestProducer {
//    private final KafkaTemplate<String, PaymentReceived> kafkaTemplate;
//    public void sendTestMessage()
//    {
//        PaymentReceived payload = PaymentReceived.newBuilder()
//            .setPaymentId(UUID.randomUUID().toString())
//            .setClientId("test-client")
//            .setAmount(new DecimalConversion().toBytes(
//                BigDecimal.ONE,
//                null,
//                PaymentReceived.getClassSchema().getField("amount").schema()))
//            .setCurrency("USD")
//            .setStatus("RECEIVED")
//                .build();
//        kafkaTemplate.send("payment-events", payload.getPaymentId().toString(), payload);
//    }
//}
