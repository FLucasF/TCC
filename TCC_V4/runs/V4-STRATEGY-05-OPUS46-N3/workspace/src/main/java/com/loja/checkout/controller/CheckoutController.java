package com.loja.checkout.controller;

import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> calcularResumo(@RequestBody ResumoRequest request) {
        try {
            return ResponseEntity.ok(service.calcular(request));
        } catch (CheckoutException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getCodigo()));
        }
    }
}
