package com.loja.checkout.api;

import com.loja.checkout.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint que o site chama para calcular o resumo da compra.
 */
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
}
