package com.cashpro.payment_processing_service.Configuration;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class Retry {
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object,Object>template)
    {
        DeadLetterPublishingRecoverer recoverer=new DeadLetterPublishingRecoverer(
                template,(record,exception)->

            new TopicPartition("payment-processed"+"-dlq",record.partition()));
            FixedBackOff backOff=new FixedBackOff(1000L,2L);
        return new DefaultErrorHandler(recoverer,backOff);
    }
}
