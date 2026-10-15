package com.loja.checkout;

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
    public CheckoutResponse resumo(@RequestBody CheckoutRequest request) {
        return service.calcular(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public Map<String, String> handleCheckout(CheckoutException e) {
        return Map.of("erro", e.getCodigo());
    }
}
