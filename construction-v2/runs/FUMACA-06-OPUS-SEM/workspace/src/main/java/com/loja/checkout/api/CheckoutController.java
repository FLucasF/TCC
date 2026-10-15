package com.loja.checkout.api;

import com.loja.checkout.dominio.CalculadoraDeResumo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraDeResumo calculadora;

    public CheckoutController(CalculadoraDeResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoPedidoResponse resumo(@RequestBody(required = false) ResumoPedidoRequest request) {
        return calculadora.calcular(request);
    }
}
