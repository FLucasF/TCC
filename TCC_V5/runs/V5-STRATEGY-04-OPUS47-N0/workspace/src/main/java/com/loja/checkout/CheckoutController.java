package com.loja.checkout;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest req) {
        return ResponseEntity.ok(service.calcular(req));
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handle(CheckoutException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.codigo()));
    }
}
