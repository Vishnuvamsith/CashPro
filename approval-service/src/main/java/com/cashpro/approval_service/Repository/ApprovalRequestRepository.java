package com.cashpro.approval_service.Repository;

import com.cashpro.approval_service.Entity.ApprovalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    @Modifying
    @Query(value = "insert into approval.approval_requests "
        + "(payment_id, maker_id, client_id, amount, currency, status, created_at, updated_at) "
        + "values (:paymentId, :makerId, :clientId, :amount, :currency, 'PENDING_APPROVAL', :createdAt, :createdAt) "
        + "on conflict (payment_id) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("paymentId") UUID paymentId,
               @Param("makerId") String makerId,
               @Param("clientId") String clientId,
               @Param("amount") java.math.BigDecimal amount,
               @Param("currency") String currency,
               @Param("createdAt") java.time.Instant createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ApprovalRequest request where request.paymentId = :paymentId")
    Optional<ApprovalRequest> findByPaymentIdForUpdate(@Param("paymentId") UUID paymentId);
}
