package com.cashpro.payment_service.DTO;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;
@Builder
public record Payload(
        UUID paymentId,
        String clientId,
        BigDecimal amount,
        String currency,
        String status

) {
}
