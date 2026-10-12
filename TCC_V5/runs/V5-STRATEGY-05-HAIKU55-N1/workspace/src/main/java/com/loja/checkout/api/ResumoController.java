package com.loja.checkout.api;

import com.loja.checkout.dominio.Resumo;
import com.loja.checkout.servico.CalculadoraResumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody ResumoRequest pedido) {
        return calculadora.calcular(pedido);
    }
}
