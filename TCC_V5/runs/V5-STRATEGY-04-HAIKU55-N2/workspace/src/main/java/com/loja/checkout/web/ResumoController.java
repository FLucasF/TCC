package com.loja.checkout.web;

import com.loja.checkout.calculo.CalculadoraResumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody RequisicaoResumo requisicao) {
        return calculadora.calcular(requisicao);
    }
}
