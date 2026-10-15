package com.loja.checkout.controller;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest req) {
        try {
            CheckoutResponse response = service.calcular(req);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getCodigo()));
        }
    }
}
