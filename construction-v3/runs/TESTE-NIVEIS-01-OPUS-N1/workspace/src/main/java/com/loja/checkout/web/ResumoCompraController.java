package com.loja.checkout.web;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.aplicacao.PedidoCheckout;
import com.loja.checkout.aplicacao.ResumoCompra;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ResumoCompraController {

    private final CalculadoraResumo calculadora;

    ResumoCompraController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    ResumoCompra resumo(@RequestBody PedidoCheckout pedido) {
        return calculadora.calcular(pedido);
    }
}
