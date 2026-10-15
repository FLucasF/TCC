package com.loja.checkout.web;

import com.loja.checkout.servico.CalculadoraResumo;
import com.loja.checkout.servico.ResultadoCheckout;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest request) {
        return switch (calculadora.calcular(request)) {
            case ResultadoCheckout.Sucesso s -> ResponseEntity.ok(s.resposta());
            case ResultadoCheckout.Falha f -> ResponseEntity.badRequest().body(Map.of("erro", f.codigo().name()));
        };
    }
}
