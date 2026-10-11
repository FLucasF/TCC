package com.loja.checkout.web;

import com.loja.checkout.resumo.CalculadoraResumo;
import com.loja.checkout.resumo.PedidoRequest;
import com.loja.checkout.resumo.ResumoCompra;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody PedidoRequest pedido) {
        return calculadora.calcular(pedido);
    }
}
