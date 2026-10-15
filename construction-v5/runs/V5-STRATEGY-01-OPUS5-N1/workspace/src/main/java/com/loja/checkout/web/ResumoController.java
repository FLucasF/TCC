package com.loja.checkout.web;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping(path = "/checkout/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoResponse resumo(@RequestBody(required = false) PedidoRequest pedido) {
        return calculadora.calcular(pedido == null
                ? new PedidoRequest(null, null, null, null, null, null, null)
                : pedido);
    }
}
