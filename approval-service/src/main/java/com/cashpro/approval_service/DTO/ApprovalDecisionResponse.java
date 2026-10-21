package com.cashpro.approval_service.DTO;

import com.cashpro.approval_service.Entity.ApprovalDecisionType;
import com.cashpro.approval_service.Entity.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record ApprovalDecisionResponse(
        UUID paymentId,
        String makerId,
        String approverId,
        ApprovalDecisionType decision,
        ApprovalStatus status,
        Instant decidedAt,
        String reason
) {
}
