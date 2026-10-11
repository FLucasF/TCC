package com.loja.controller;

import com.loja.dto.CheckoutRequest;
import com.loja.exception.CheckoutException;
import com.loja.service.CheckoutService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        try {
            return ResponseEntity.ok(checkoutService.calcular(request));
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getCodigo()));
        }
    }
}
