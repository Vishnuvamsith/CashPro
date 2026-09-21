package com.cashpro.payment_processing_service.Configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {

    @Bean
    NewTopic paymentProcessedTopic() {
        return TopicBuilder.name("payment-processed")
                .partitions(1)
                .replicas(2)
                .build();
    }
}