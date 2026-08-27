package com.cashpro.payment_service.Controller;

import com.cashpro.payment_service.DTO.CreatePaymentRequest;
import com.cashpro.payment_service.DTO.PaymentResponse;
import com.cashpro.payment_service.Kafka.KafkaTestProducer;
import com.cashpro.payment_service.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;
    private final KafkaTestProducer kafkaTestProducer;

    @PostMapping
    public PaymentResponse create(
            @RequestHeader("Idempotency-Key") String idemKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        return service.createPayment(request, idemKey);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/send/event")
    public String sendMessage()
    {
        kafkaTestProducer.sendTestMessage();
        return "message sent successfully";
    }
}
