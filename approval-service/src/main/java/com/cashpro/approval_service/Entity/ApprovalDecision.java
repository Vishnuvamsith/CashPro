package com.cashpro.approval_service.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_decisions", schema = "approval")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "maker_id", nullable = false, length = 100)
    private String makerId;

    @Column(name = "approver_id", nullable = false, length = 100)
    private String approverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecisionType decision;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(length = 500)
    private String reason;
}
