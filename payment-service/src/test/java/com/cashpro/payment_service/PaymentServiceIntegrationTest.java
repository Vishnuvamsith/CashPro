package com.cashpro.payment_service;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.kafka.clients.admin.NewTopic;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.cashpro.payment_service.DTO.CreatePaymentRequest;
import com.cashpro.payment_service.DTO.PaymentResponse;
import com.cashpro.payment_service.Entity.PaymentStatus;
import com.cashpro.payment_service.Repo.OutboxEventRepository;
import com.cashpro.payment_service.Repo.PaymentRepository;
import com.cashpro.payment_service.Service.PaymentService;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class PaymentServiceIntegrationTest {

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        NewTopic paymentProcessedTopic() {
            return TopicBuilder.name("payment-events")
                    .partitions(3)
                    .replicas(1)
                    .build();
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("cashpro")
                    .withUsername("cashpro")
                    .withPassword("cashpro");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.9.0")
            );

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void shouldCreatePaymentAndPersistOutboxEvent() {
        String idemKey = "integration-" + UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                "client-123",
                "debit-acc",
                "credit-acc",
                new BigDecimal("125.50"),
                "USD"
        );

        PaymentResponse response = paymentService.createPayment(request, idemKey);

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("125.50"));
        assertThat(response.status()).isEqualTo(PaymentStatus.RECEIVED);
        assertThat(paymentRepository.findById(response.paymentId())).isPresent();
        assertThat(outboxEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getEventType()).isEqualTo("PAYMENT_RECEIVED");
                    assertThat(event.getStatus()).isEqualTo("PENDING");
                });
    }
}