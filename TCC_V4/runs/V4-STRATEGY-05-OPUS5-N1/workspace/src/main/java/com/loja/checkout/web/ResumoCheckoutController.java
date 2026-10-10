package com.loja.checkout.web;

import com.loja.checkout.CalculadoraResumo;
import com.loja.checkout.RequisicaoResumo;
import com.loja.checkout.ResumoCheckout;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoCheckoutController {

    private final CalculadoraResumo calculadora;

    public ResumoCheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCheckout resumo(@RequestBody(required = false) RequisicaoResumo requisicao) {
        return calculadora.calcular(requisicao);
    }
}
