package com.cashpro.payment_service.DTO;

import com.cashpro.payment_service.Entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;
@Builder
public record Payload(
        @NotBlank
        UUID paymentId,
        @NotBlank
        String clientId,
        @NotBlank
        BigDecimal amount,
        @NotBlank
        String currency,
        @NotBlank
        String status

) {
}
