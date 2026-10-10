package com.loja.checkout.web;

import com.loja.checkout.aplicacao.CalculadoraDeResumo;
import com.loja.checkout.aplicacao.PedidoDoSite;
import com.loja.checkout.aplicacao.ResumoDaCompra;
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

    @PostMapping("/resumo")
    public ResumoDaCompra resumo(@RequestBody(required = false) PedidoDoSite pedido) {
        return calculadora.calcular(pedido == null ? pedidoVazio() : pedido);
    }

    private PedidoDoSite pedidoVazio() {
        return new PedidoDoSite(null, null, null, null, null, null, null);
    }
}
