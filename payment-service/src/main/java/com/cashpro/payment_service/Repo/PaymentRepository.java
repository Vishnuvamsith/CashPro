package com.cashpro.payment_service.Repo;

import com.cashpro.payment_service.Entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Payment findByIdempotencyKey(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.paymentId = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") UUID paymentId);
}
