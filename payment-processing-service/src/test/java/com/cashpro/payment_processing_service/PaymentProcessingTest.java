package com.cashpro.payment_processing_service;

import org.apache.kafka.clients.admin.NewTopic;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class PaymentProcessingTest {

    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        NewTopic paymentProcessedTopic() {
            return TopicBuilder.name("payment-processed")
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.9.0")
            );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void contextLoads() {
        assertThatCode(() -> {}).doesNotThrowAnyException();
    }
}