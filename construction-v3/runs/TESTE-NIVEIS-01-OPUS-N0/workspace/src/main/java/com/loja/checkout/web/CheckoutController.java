package com.loja.checkout.web;

import com.loja.checkout.resumo.CalculadoraResumo;
import com.loja.checkout.resumo.ResumoCompra;
import com.loja.checkout.resumo.SolicitacaoResumo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint que o site chama para montar o resumo da compra. */
@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadoraResumo;

    public CheckoutController(CalculadoraResumo calculadoraResumo) {
        this.calculadoraResumo = calculadoraResumo;
    }

    @PostMapping(path = "/checkout/resumo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoCompra resumo(@RequestBody SolicitacaoResumo solicitacao) {
        return calculadoraResumo.calcular(solicitacao);
    }
}
