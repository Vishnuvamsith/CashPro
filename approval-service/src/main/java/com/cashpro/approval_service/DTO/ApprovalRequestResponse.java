package com.cashpro.approval_service.DTO;

import com.cashpro.approval_service.Entity.ApprovalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApprovalRequestResponse(
        UUID paymentId,
        String makerId,
        String clientId,
        BigDecimal amount,
        String currency,
        ApprovalStatus status,
        Instant createdAt
) {
}
