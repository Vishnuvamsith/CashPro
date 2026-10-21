package com.cashpro.approval_service.Service;

import com.cashpro.approval_service.Configuration.ApprovalProperties;
import com.cashpro.approval_service.DTO.ApprovalDecisionResponse;
import com.cashpro.approval_service.DTO.ApprovalRequestResponse;
import com.cashpro.approval_service.Entity.ApprovalDecision;
import com.cashpro.approval_service.Entity.ApprovalDecisionType;
import com.cashpro.approval_service.Entity.ApprovalOutboxEvent;
import com.cashpro.approval_service.Entity.ApprovalRequest;
import com.cashpro.approval_service.Entity.ApprovalStatus;
import com.cashpro.approval_service.Repository.ApprovalDecisionRepository;
import com.cashpro.approval_service.Repository.ApprovalOutboxRepository;
import com.cashpro.approval_service.Repository.ApprovalRequestRepository;
import com.cashpro.events.ApprovalDecisionRecorded;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    private final ApprovalRequestRepository requests;
    private final ApprovalDecisionRepository decisions;
    private final ApprovalOutboxRepository outbox;
    private final ApprovalProperties properties;

    @Transactional
    public void registerRequest(UUID paymentId, String makerId, String clientId, BigDecimal amount, String currency) {
        Instant now = Instant.now();
        requests.insertIfAbsent(paymentId, makerId, clientId, amount, currency, now);
    }

    @Transactional
    public ApprovalDecisionResponse approve(UUID paymentId, String checkerId, String reason) {
        return decide(paymentId, checkerId, reason, ApprovalDecisionType.APPROVED);
    }

    @Transactional
    public ApprovalDecisionResponse reject(UUID paymentId, String checkerId, String reason) {
        return decide(paymentId, checkerId, reason, ApprovalDecisionType.REJECTED);
    }

    @Transactional
    public ApprovalRequestResponse getRequest(UUID paymentId) {
        ApprovalRequest request = requests.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));
        return new ApprovalRequestResponse(request.getPaymentId(), request.getMakerId(), request.getClientId(),
                request.getAmount(), request.getCurrency(), request.getStatus(), request.getCreatedAt());
    }

    @Transactional
    public ApprovalDecisionResponse getDecision(UUID paymentId) {
        ApprovalDecision decision = decisions.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval decision not found"));
        return toResponse(decision);
    }

    private ApprovalDecisionResponse decide(UUID paymentId, String checkerId, String reason,
                                             ApprovalDecisionType decisionType) {
        if (!properties.isAuthorized(checkerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not an authorized approver");
        }
        ApprovalRequest request = requests.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found"));
        if (request.getStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approval request is no longer pending");
        }
        String normalizedChecker = checkerId.trim();
        if (request.getMakerId().equals(normalizedChecker)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Maker cannot approve their own payment");
        }

        Instant decidedAt = Instant.now();
        ApprovalStatus nextStatus = decisionType == ApprovalDecisionType.APPROVED
                ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
        request.setStatus(nextStatus);
        request.setUpdatedAt(decidedAt);
        requests.save(request);
        ApprovalDecision decision = decisions.save(ApprovalDecision.builder()
                .paymentId(paymentId).makerId(request.getMakerId()).approverId(normalizedChecker)
                .decision(decisionType).decidedAt(decidedAt)
                .reason(reason == null || reason.isBlank() ? null : reason.trim()).build());

        ApprovalDecisionRecorded event = ApprovalDecisionRecorded.newBuilder()
                .setPaymentId(paymentId).setMakerId(request.getMakerId()).setApproverId(normalizedChecker)
                .setDecision(decisionType.name()).setDecidedAtEpochMillis(decidedAt.toEpochMilli())
                .setReason(decision.getReason()).build();
        outbox.save(ApprovalOutboxEvent.builder().eventId(UUID.randomUUID()).aggregateId(paymentId)
            .eventType("APPROVAL_DECISION_RECORDED").payload(toJson(event))
                .status("PENDING").createdAt(decidedAt).build());
        return toResponse(decision);
    }

    private ApprovalDecisionResponse toResponse(ApprovalDecision decision) {
        return new ApprovalDecisionResponse(decision.getPaymentId(), decision.getMakerId(), decision.getApproverId(),
                decision.getDecision(), decision.getDecision() == ApprovalDecisionType.APPROVED
                ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED, decision.getDecidedAt(), decision.getReason());
    }

    private ObjectNode toJson(ApprovalDecisionRecorded event) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode()
                .put("paymentId", event.getPaymentId().toString())
                .put("makerId", event.getMakerId())
                .put("approverId", event.getApproverId())
                .put("decision", event.getDecision())
                .put("decidedAtEpochMillis", event.getDecidedAtEpochMillis());
        if (event.getReason() == null) {
            payload.putNull("reason");
        } else {
            payload.put("reason", event.getReason());
        }
        return payload;
    }
}
