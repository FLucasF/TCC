package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.Resumo;
import com.loja.checkout.dominio.Solicitacao;
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
    public Resumo resumo(@RequestBody(required = false) Solicitacao solicitacao) {
        return calculadora.calcular(solicitacao == null ? vazia() : solicitacao);
    }

    private Solicitacao vazia() {
        return new Solicitacao(null, null, null, null, null, null, null);
    }
}
