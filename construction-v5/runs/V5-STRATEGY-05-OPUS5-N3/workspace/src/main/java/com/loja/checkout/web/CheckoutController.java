package com.loja.checkout.web;

import com.loja.checkout.CalculadoraDeResumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraDeResumo calculadora;

    public CheckoutController(CalculadoraDeResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return calculadora.calcular(pedido);
    }
}
