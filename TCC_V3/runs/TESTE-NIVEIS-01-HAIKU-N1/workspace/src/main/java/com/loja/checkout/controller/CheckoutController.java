package com.loja.checkout.controller;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.service.CheckoutService;
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
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        Object resultado = checkoutService.processar(request);
        return ResponseEntity.ok(resultado);
    }
}
