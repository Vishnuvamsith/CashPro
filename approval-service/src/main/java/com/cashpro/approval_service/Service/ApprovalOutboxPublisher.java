package com.cashpro.approval_service.Service;

import com.cashpro.approval_service.Entity.ApprovalOutboxEvent;
import com.cashpro.approval_service.Repository.ApprovalOutboxRepository;
import com.cashpro.events.ApprovalDecisionRecorded;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ApprovalOutboxPublisher {
    private final ApprovalOutboxRepository repository;
    private final KafkaTemplate<String, ApprovalDecisionRecorded> kafkaTemplate;

    @Scheduled(fixedDelay = 2000)
    public void publishPending() {
        repository.findTop100ByStatusOrderByCreatedAtAsc("PENDING").forEach(event -> {
            try {
                com.fasterxml.jackson.databind.JsonNode payloadNode = event.getPayload();
                ApprovalDecisionRecorded payload = ApprovalDecisionRecorded.newBuilder()
                    .setPaymentId(java.util.UUID.fromString(payloadNode.get("paymentId").asText()))
                    .setMakerId(payloadNode.get("makerId").asText())
                    .setApproverId(payloadNode.get("approverId").asText())
                    .setDecision(payloadNode.get("decision").asText())
                    .setDecidedAtEpochMillis(payloadNode.get("decidedAtEpochMillis").asLong())
                    .setReason(payloadNode.hasNonNull("reason") ? payloadNode.get("reason").asText() : null)
                    .build();
                kafkaTemplate.send("approval-decisions", event.getAggregateId().toString(), payload)
                        .whenComplete((result, exception) -> {
                            if (exception == null) {
                                repository.markPublishedIfPending(event.getEventId(), Instant.now());
                            }
                        });
            } catch (Exception exception) {
                // Keep failed records pending for investigation and retry.
            }
        });
    }

}
