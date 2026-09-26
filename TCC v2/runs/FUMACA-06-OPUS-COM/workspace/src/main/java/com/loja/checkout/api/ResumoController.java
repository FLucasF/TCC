package com.loja.checkout.api;

import com.loja.checkout.dominio.CalculadoraDeResumo;
import com.loja.checkout.dominio.ResumoCompra;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final CalculadoraDeResumo calculadora;

    public ResumoController(CalculadoraDeResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping(path = "/checkout/resumo", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResumoCompra resumo(@RequestBody ResumoRequest request) {
        return calculadora.calcular(request.paraSolicitacao());
    }
}
