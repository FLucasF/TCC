package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.dominio.SolicitacaoResumo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoDaCompraController {

    private final CalculadoraResumo calculadora;

    public ResumoDaCompraController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping(path = "/checkout/resumo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoCompra resumir(@RequestBody SolicitacaoResumo solicitacao) {
        return calculadora.calcular(solicitacao);
    }
}
