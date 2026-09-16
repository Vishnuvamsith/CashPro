package com.cashpro.payment_service.Entity;

import com.cashpro.payment_service.DTO.Payload;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Payload payload;

    @Column(name="status", nullable = false)
    private String status;

    @CreatedDate
    @Column(name="created_at",nullable = false)
    private Instant createdAt;

    @Column(name="published_at")
    private Instant publishedAt;
}