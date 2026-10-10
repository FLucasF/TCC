package com.loja.checkout.controller;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/checkout/resumo")
    public CheckoutResponse resumo(@RequestBody CheckoutRequest request) {
        return checkoutService.calcularResumo(request);
    }
}
