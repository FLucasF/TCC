package com.loja.checkout.web;

import com.loja.checkout.servico.CalculoResumoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculoResumoService calculoResumoService;

    public CheckoutController(CalculoResumoService calculoResumoService) {
        this.calculoResumoService = calculoResumoService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest requisicao) {
        return calculoResumoService.calcular(requisicao);
    }
}
