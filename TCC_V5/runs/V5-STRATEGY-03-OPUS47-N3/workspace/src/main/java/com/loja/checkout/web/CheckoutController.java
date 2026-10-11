package com.loja.checkout.web;

import com.loja.checkout.erro.ErroPedido;
import com.loja.checkout.servico.CalculadoraResumo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody ResumoRequest req) {
        try {
            return ResponseEntity.ok(calculadora.calcular(req));
        } catch (ErroPedido e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getCodigo()));
        }
    }
}
