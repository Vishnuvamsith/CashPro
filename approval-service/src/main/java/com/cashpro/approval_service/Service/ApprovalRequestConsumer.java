package com.cashpro.approval_service.Service;

import com.cashpro.events.ApprovalRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalRequestConsumer {
    private final ApprovalService approvalService;

    @KafkaListener(topics = "approval-requests", groupId = "approval-service-requests")
    public void onRequest(ApprovalRequested event) {
        if (event == null || event.getPaymentId() == null || event.getMakerId() == null
                || event.getMakerId().isBlank() || event.getClientId() == null
                || event.getAmount() == null || event.getCurrency() == null) {
            throw new IllegalArgumentException("Approval request event is incomplete");
        }
        approvalService.registerRequest(event.getPaymentId(), event.getMakerId(), event.getClientId(),
                event.getAmount(), event.getCurrency());
    }
}
