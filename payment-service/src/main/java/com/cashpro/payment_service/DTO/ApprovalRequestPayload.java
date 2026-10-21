package com.cashpro.payment_service.DTO;

import java.math.BigDecimal;
import java.util.UUID;

public record ApprovalRequestPayload(
        UUID paymentId,
        String makerId,
        String clientId,
        BigDecimal amount,
        String currency
) {
}
