package com.loja.checkout.api;

import com.loja.checkout.calculo.CalculadoraResumo;
import com.loja.checkout.calculo.ResumoCompra;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ResumoController {

    private final CalculadoraResumo calculadora;

    ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    ResumoCompra resumo(@RequestBody ResumoRequest request) {
        return calculadora.calcular(request);
    }
}
