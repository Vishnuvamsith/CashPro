package com.cashpro.payment_processing_service.Repository;

import com.cashpro.payment_processing_service.Entity.PaymentProcessedModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface payloadRepo extends JpaRepository<PaymentProcessedModel, UUID> {

}
