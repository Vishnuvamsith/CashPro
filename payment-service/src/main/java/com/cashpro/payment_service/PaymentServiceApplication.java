package com.cashpro.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableKafka
public class PaymentServiceApplication {

	public static void main(String[] args) {
		System.out.println("KAFKA_BOOTSTRAP_SERVERS = "
				+ System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
