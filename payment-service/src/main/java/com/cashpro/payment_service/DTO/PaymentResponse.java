package com.cashpro.payment_service.DTO;

import com.cashpro.payment_service.Entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt
) {}
