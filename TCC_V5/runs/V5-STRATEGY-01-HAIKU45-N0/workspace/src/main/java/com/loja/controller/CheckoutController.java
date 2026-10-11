package com.loja.controller;

import com.loja.dto.CheckoutError;
import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
        try {
            CheckoutResponse response = checkoutService.calcular(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutService.CheckoutException e) {
            CheckoutError error = new CheckoutError(e.getCodigo());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
