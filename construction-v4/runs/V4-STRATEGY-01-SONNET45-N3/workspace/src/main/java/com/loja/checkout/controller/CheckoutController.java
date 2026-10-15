package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.servico.CalculadoraResumo;
import com.loja.checkout.servico.ErroNegocioException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody PedidoRequest pedido) {
        try {
            ResumoResponse resumo = calculadora.calcular(pedido);
            return ResponseEntity.ok(resumo);
        } catch (ErroNegocioException e) {
            return ResponseEntity.ok(new ErroResponse(e.getCodigo()));
        }
    }
}
