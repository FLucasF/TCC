package com.loja.checkout.api;

import com.loja.checkout.aplicacao.CalculadoraDoResumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraDoResumo calculadora;

    public CheckoutController(CalculadoraDoResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest pedido) {
        return calculadora.calcular(pedido);
    }
}
