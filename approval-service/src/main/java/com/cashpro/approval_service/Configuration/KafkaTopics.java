package com.cashpro.approval_service.Configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {
    @Bean
    NewTopic approvalRequestsTopic() {
        return TopicBuilder.name("approval-requests").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic approvalDecisionsTopic() {
        return TopicBuilder.name("approval-decisions").partitions(3).replicas(1).build();
    }
}
