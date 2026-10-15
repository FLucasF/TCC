package com.loja.checkout.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CalculadoraResumo;

@RestController
public class CheckoutController {
    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse calcularResumo(@RequestBody ResumoRequest requisicao) {
        return calculadora.calcular(
            requisicao.getItens(),
            requisicao.getModalidadeEntrega(),
            requisicao.getCupom(),
            requisicao.getFormaPagamento(),
            requisicao.getParcelas(),
            requisicao.getNivelClube(),
            requisicao.getRegiao()
        );
    }
}
