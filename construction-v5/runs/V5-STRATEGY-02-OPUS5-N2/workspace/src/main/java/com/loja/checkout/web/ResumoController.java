package com.loja.checkout.web;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.aplicacao.PedidoRecebido;
import com.loja.checkout.aplicacao.ResumoCompra;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusado;
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
    public ResumoCompra resumo(@RequestBody(required = false) PedidoRecebido pedido) {
        if (pedido == null) {
            throw new PedidoRecusado(CodigoErro.PEDIDO_INVALIDO);
        }
        return calculadora.calcular(pedido);
    }
}
