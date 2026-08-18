package com.cashpro.payment_service.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest
(@NotBlank
 String clientId,
 @NotBlank
 String debitAccount,
 @NotBlank
 String creditAccount,
 @NotNull
 @DecimalMin(value = "0.01", message = "Amount must be greater than 0.01")
 BigDecimal amount,
 @NotBlank
 @Size(min=3,max=3)
 String currency
){}
