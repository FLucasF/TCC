package com.loja.checkout.web;

import com.loja.checkout.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe os dados da compra e devolve o resumo calculado.
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
