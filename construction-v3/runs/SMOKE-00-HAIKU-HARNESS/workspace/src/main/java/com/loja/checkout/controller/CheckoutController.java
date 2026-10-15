package com.loja.checkout.controller;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumoCheckout(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            Map<String, String> error = new HashMap<>();
            error.put("erro", e.getCodigo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
