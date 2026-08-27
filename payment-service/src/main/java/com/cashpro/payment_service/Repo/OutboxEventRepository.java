package com.cashpro.payment_service.Repo;

import com.cashpro.payment_service.Entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent>findAllByStatus(String status);
    //List<OutboxEvent>findAll();
}