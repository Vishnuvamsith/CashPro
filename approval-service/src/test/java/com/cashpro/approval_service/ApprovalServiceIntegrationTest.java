package com.cashpro.approval_service;

import com.cashpro.approval_service.Entity.ApprovalDecisionType;
import com.cashpro.approval_service.Entity.ApprovalStatus;
import com.cashpro.approval_service.Repository.ApprovalDecisionRepository;
import com.cashpro.approval_service.Repository.ApprovalOutboxRepository;
import com.cashpro.approval_service.Repository.ApprovalRequestRepository;
import com.cashpro.approval_service.Service.ApprovalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = "cashpro.approval.authorized-approvers=checker-1,checker-2")
class ApprovalServiceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("cashpro")
            .withUsername("cashpro")
            .withPassword("cashpro");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.9.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url", () -> "http://localhost:8088");
    }

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalRequestRepository requestRepository;

    @Autowired
    private ApprovalDecisionRepository decisionRepository;

    @Autowired
    private ApprovalOutboxRepository outboxRepository;

    @Test
    void persistsApprovalRequestDecisionAndTransactionalOutbox() {
        UUID paymentId = UUID.randomUUID();
        approvalService.registerRequest(paymentId, "maker-1", "client-1",
                new BigDecimal("1250.00"), "USD");
        approvalService.registerRequest(paymentId, "maker-1", "client-1",
                new BigDecimal("1250.00"), "USD");

        var result = approvalService.approve(paymentId, "checker-1", "reviewed");

        assertThat(result.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(requestRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(ApprovalStatus.APPROVED);
        assertThat(decisionRepository.findByPaymentId(paymentId)).isPresent();
        assertThat(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING"))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getAggregateId()).isEqualTo(paymentId);
                    assertThat(event.getEventType()).isEqualTo("APPROVAL_DECISION_RECORDED");
                });
    }
}
