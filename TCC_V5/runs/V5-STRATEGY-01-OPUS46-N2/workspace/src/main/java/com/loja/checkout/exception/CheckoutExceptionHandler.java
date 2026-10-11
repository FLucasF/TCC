package com.loja.checkout.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handleCheckout(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("erro", ex.getCodigo()));
    }
}
