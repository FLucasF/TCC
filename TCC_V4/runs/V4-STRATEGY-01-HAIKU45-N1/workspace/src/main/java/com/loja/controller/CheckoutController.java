package com.loja.controller;

import com.loja.dto.CheckoutRequestDto;
import com.loja.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequestDto request) {
        Object resultado = checkoutService.calcularResumo(request);
        return ResponseEntity.ok(resultado);
    }
}
