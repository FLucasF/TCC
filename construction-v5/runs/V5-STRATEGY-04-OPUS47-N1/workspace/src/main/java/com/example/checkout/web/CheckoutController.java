package com.example.checkout.web;

import com.example.checkout.model.ResumoRequest;
import com.example.checkout.model.ResumoResponse;
import com.example.checkout.service.CheckoutException;
import com.example.checkout.service.CheckoutService;
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
    public ResumoResponse resumo(@RequestBody ResumoRequest req) {
        return service.calcular(req);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handle(CheckoutException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getCodigo()));
    }
}
