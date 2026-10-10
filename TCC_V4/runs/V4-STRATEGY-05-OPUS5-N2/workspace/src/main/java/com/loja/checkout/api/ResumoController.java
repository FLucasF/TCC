package com.loja.checkout.api;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Resumo;
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

    @PostMapping(path = "/checkout/resumo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Resumo resumo(@RequestBody Pedido pedido) {
        return calculadora.calcular(pedido);
    }
}
