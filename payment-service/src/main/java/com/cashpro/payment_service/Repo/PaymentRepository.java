package com.cashpro.payment_service.Repo;

import com.cashpro.payment_service.Entity.Payment;
import com.cashpro.payment_service.PaymentServiceApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Payment findByIdempotencyKey(String key);
}
