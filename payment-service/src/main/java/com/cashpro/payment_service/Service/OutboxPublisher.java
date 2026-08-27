package com.cashpro.payment_service.Service;

import com.cashpro.payment_service.Repo.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        repository.findAll()
                .stream()
                .filter(event -> event.getStatus().equals("PENDING"))
                .forEach(event -> {

                    kafkaTemplate.send(
                            "payment-events",
                            event.getAggregateId().toString(),
                            event.getPayload()
                    ).whenComplete((result, exception) -> {

                        if (exception == null) {

                            event.setStatus("PUBLISHED");
                            event.setPublishedAt(Instant.now());

                            repository.save(event);

                        } else {

                            System.out.println(
                                    "Failed to publish event: "
                                            + event.getId()
                            );
                        }
                    });
                });
    }
}