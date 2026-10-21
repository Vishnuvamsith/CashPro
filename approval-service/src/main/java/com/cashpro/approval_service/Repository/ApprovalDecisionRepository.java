package com.cashpro.approval_service.Repository;

import com.cashpro.approval_service.Entity.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, UUID> {
    Optional<ApprovalDecision> findByPaymentId(UUID paymentId);
}
