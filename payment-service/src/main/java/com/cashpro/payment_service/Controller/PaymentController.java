package com.cashpro.payment_service.Controller;

import com.cashpro.payment_service.DTO.CreatePaymentRequest;
import com.cashpro.payment_service.DTO.PaymentResponse;
//import com.cashpro.payment_service.Kafka.KafkaTestProducer;
import com.cashpro.payment_service.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import com.cashpro.payment_service.Configuration.ApprovalPolicyProperties;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;
    private final ApprovalPolicyProperties prop;
    //private final KafkaTestProducer kafkaTestProducer;

    @PostMapping
    public PaymentResponse create(
            @RequestHeader("Idempotency-Key") String idemKey,
            @RequestHeader("X-User-Id") String makerId,
            @Valid @RequestBody CreatePaymentRequest request) {

        return service.createPayment(request, idemKey, makerId);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/thresholds/{currency}/{threshold}")
    public ResponseEntity<String> setThreshold(
            @PathVariable String currency,
            @PathVariable String threshold) {

        BigDecimal value;
        try {
            value = new BigDecimal(threshold);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("threshold must be a number");
        }
        if (value.signum() < 0) {
            return ResponseEntity.badRequest().body("threshold must be >= 0");
        }

        prop.setThresholds(Map.of(currency.toUpperCase(Locale.ROOT), value));
        return ResponseEntity.ok("Approval threshold set to: " + value);
    }

//    @PostMapping("/send/event")
//    public String sendMessage()
//    {
//        kafkaTestProducer.sendTestMessage();
//        return "message sent successfully";
//    }
}
