package com.loja.checkout.web;

import com.loja.checkout.service.CheckoutService;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
    public Object resumo(@RequestBody(required = false) CheckoutRequest req) {
        if (req == null) {
            return Map.of("erro", "PEDIDO_INVALIDO");
        }
        try {
            return service.calcular(req);
        } catch (CheckoutException e) {
            return Map.of("erro", e.getMessage());
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, String> parseError() {
        return Map.of("erro", "PEDIDO_INVALIDO");
    }
}
