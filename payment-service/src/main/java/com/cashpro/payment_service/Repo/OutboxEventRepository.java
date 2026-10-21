package com.cashpro.payment_service.Repo;

import com.cashpro.payment_service.Entity.OutboxEvent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent>findAllByStatus(String status);
        @Modifying
        @Transactional
        @Query("update OutboxEvent event set event.status = 'PUBLISHED', event.publishedAt = :publishedAt "
            + "where event.id = :eventId and event.status = 'PENDING'")
        int markPublishedIfPending(@Param("eventId") UUID eventId, @Param("publishedAt") java.time.Instant publishedAt);
    //List<OutboxEvent>findAll();
}