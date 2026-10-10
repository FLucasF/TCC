package com.loja.checkout.controller;

import com.loja.checkout.dto.ResumoCheckoutRequest;
import com.loja.checkout.dto.ResumoCheckoutResponse;
import com.loja.checkout.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResumoCheckoutResponse resumo(@RequestBody ResumoCheckoutRequest request) {
        return checkoutService.calcularResumo(request);
    }
}
