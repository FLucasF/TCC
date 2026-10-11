package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutRequest;
import com.loja.checkout.domain.CheckoutResponse;
import com.loja.checkout.service.CheckoutError;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            CheckoutResponse response = service.calculate(req);
            return ResponseEntity.ok(response);
        } catch (CheckoutError e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getCode()));
        }
    }
}
