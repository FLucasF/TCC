package com.loja.checkout.api;

import com.loja.checkout.regra.CalculadoraResumo;
import com.loja.checkout.regra.PedidoCompra;
import com.loja.checkout.regra.ResumoCompra;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody PedidoCompra pedido) {
        return calculadora.calcular(pedido);
    }
}
