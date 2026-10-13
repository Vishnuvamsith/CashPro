package com.cashpro.payment_processing_service.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_processing")
@Builder
public class PaymentProcessedModel {
    @Id
    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID payment_id;
    @Column(name = "client_id", nullable = false)
    private String client_id;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "currency", nullable = false)
    private String currency;
    @Column(name = "status", nullable = false)
    private String status;

}
