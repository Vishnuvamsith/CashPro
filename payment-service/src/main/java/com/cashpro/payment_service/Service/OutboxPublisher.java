package com.cashpro.payment_service.Service;

import com.cashpro.events.ApprovalRequested;
import com.cashpro.events.PaymentReceived;
import com.cashpro.payment_service.Repo.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        repository.findAll()
                .stream()
                .filter(event -> event.getStatus().equals("PENDING"))
                .forEach(event -> {
                    JsonNode payload = event.getPayload();
                    String topic = "APPROVAL_REQUESTED".equals(event.getEventType())
                        ? "approval-requests"
                        : "payment-events";
                    Object value = "APPROVAL_REQUESTED".equals(event.getEventType())
                        ? toApprovalRequested(payload)
                        : toPaymentReceived(payload);
                    kafkaTemplate.send(topic, event.getAggregateId().toString(), value)
                        .whenComplete((result, exception) -> {

                        if (exception == null) {
                            repository.markPublishedIfPending(event.getId(), Instant.now());

                        } else {

                            System.out.println(
                                    "Failed to publish event: "
                                            + event.getId()
                            );
                        }
                    });
                });
    }

    private PaymentReceived toPaymentReceived(JsonNode payload) {
        return PaymentReceived.newBuilder()
                .setPaymentId(UUID.fromString(payload.get("paymentId").asText()))
                .setClientId(payload.get("clientId").asText())
                .setAmount(new BigDecimal(payload.get("amount").asText()))
                .setCurrency(payload.get("currency").asText())
                .setStatus(payload.get("status").asText())
                .build();
    }

    private ApprovalRequested toApprovalRequested(JsonNode payload) {
        return ApprovalRequested.newBuilder()
                .setPaymentId(UUID.fromString(payload.get("paymentId").asText()))
                .setMakerId(payload.get("makerId").asText())
                .setClientId(payload.get("clientId").asText())
                .setAmount(new BigDecimal(payload.get("amount").asText()))
                .setCurrency(payload.get("currency").asText())
                .build();
    }
}