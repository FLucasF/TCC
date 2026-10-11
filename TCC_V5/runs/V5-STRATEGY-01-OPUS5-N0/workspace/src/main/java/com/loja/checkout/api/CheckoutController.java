package com.loja.checkout.api;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest pedido) {
        return calculadora.calcular(pedido == null
                ? new ResumoRequest(null, null, null, null, null, null, null)
                : pedido);
    }
}
