package com.loja.checkout.api;

import com.loja.checkout.servico.CalculoCheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculoCheckoutService calculoCheckoutService;

    public CheckoutController(CalculoCheckoutService calculoCheckoutService) {
        this.calculoCheckoutService = calculoCheckoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCheckoutResponse resumo(@RequestBody ResumoCheckoutRequest request) {
        return calculoCheckoutService.calcular(request);
    }
}
