package com.cashpro.approval_service.Repository;

import com.cashpro.approval_service.Entity.ApprovalOutboxEvent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ApprovalOutboxRepository extends JpaRepository<ApprovalOutboxEvent, UUID> {
    List<ApprovalOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);

    @Modifying
    @Transactional
    @Query(value = "update approval.outbox_events set status = 'PUBLISHED', published_at = :publishedAt "
            + "where event_id = :eventId and status = 'PENDING'", nativeQuery = true)
    int markPublishedIfPending(@Param("eventId") UUID eventId,
                               @Param("publishedAt") java.time.Instant publishedAt);
}
