package com.loja.checkout.api;

import com.loja.checkout.servico.CalculadoraResumoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumoService calculadora;

    public CheckoutController(CalculadoraResumoService calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return calculadora.calcular(request);
    }
}
