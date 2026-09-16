package com.cashpro.payment_processing_service.DTO;

import java.math.BigDecimal;
import java.util.UUID;

public record Payload(
        UUID paymentId,
        String clientId,
        BigDecimal amount,
        String currency,
        String status

) {
}
