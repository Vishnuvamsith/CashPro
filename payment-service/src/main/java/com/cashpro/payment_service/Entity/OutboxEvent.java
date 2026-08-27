package com.cashpro.payment_service.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="aggregate_type",nullable = false)
    private String aggregateType;

    @Column(name="aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name="event_type",nullable = false)
    private String eventType;

    @Column(name="payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name="status", nullable = false)
    private String status;

    @CreatedDate
    @Column(name="created_at",nullable = false)
    private Instant createdAt;

    @Column(name="published_at")
    private Instant publishedAt;
}