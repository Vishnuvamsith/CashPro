package com.cashpro.payment_service.Exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
@ControllerAdvice
public class GlobalException {
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<String> handleDuplicatePaymentException(DuplicatePaymentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
