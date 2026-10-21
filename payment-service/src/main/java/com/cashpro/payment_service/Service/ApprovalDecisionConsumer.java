package com.cashpro.payment_service.Service;

import com.cashpro.events.ApprovalDecisionRecorded;
import com.cashpro.payment_service.DTO.Payload;
import com.cashpro.payment_service.Entity.OutboxEvent;
import com.cashpro.payment_service.Entity.Payment;
import com.cashpro.payment_service.Entity.PaymentStatus;
import com.cashpro.payment_service.Repo.OutboxEventRepository;
import com.cashpro.payment_service.Repo.PaymentRepository;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApprovalDecisionConsumer {
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    @KafkaListener(topics = "approval-decisions", groupId = "payment-service-approval-decisions")
    public void onDecision(ApprovalDecisionRecorded event) {
        if (event == null || event.getPaymentId() == null || event.getDecision() == null) {
            throw new IllegalArgumentException("Approval decision event is incomplete");
        }
        Payment payment = paymentRepository.findByIdForUpdate(event.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for approval decision"));
        if (payment.getStatus() != PaymentStatus.PENDING_APPROVAL) {
            return;
        }
        if ("APPROVED".equals(event.getDecision())) {
            if (event.getDecidedAtEpochMillis() <= 0 || event.getApproverId() == null
                    || event.getApproverId().isBlank() || event.getMakerId() == null
                    || event.getMakerId().isBlank() || event.getMakerId().equals(event.getApproverId())) {
                throw new IllegalArgumentException("Approval decision audit data is invalid");
            }
            payment.setStatus(PaymentStatus.APPROVED);
            paymentRepository.save(payment);
            Payload payload = Payload.builder()
                    .paymentId(payment.getPaymentId())
                    .clientId(payment.getClientId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(payment.getStatus().name())
                    .build();
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(payment.getPaymentId())
                    .eventType("PAYMENT_RECEIVED")
                        .payload(JsonNodeFactory.instance.objectNode()
                            .put("paymentId", payload.paymentId().toString())
                            .put("clientId", payload.clientId())
                            .put("amount", payload.amount().toPlainString())
                            .put("currency", payload.currency())
                            .put("status", payload.status()))
                    .status("PENDING")
                    .build());
        } else if ("REJECTED".equals(event.getDecision())) {
            if (event.getDecidedAtEpochMillis() <= 0 || event.getApproverId() == null
                    || event.getApproverId().isBlank() || event.getMakerId() == null
                    || event.getMakerId().isBlank() || event.getMakerId().equals(event.getApproverId())) {
                throw new IllegalArgumentException("Approval decision audit data is invalid");
            }
            payment.setStatus(PaymentStatus.REJECTED);
            paymentRepository.save(payment);
        } else {
            throw new IllegalArgumentException("Unsupported approval decision: " + event.getDecision());
        }
    }
}
