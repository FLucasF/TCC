package com.loja.resumo;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoCompraController {

    private final CalculadoraResumo calculadora;

    public ResumoCompraController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompraResponse resumo(@RequestBody ResumoCompraRequest request) {
        return calculadora.calcular(request);
    }
}
