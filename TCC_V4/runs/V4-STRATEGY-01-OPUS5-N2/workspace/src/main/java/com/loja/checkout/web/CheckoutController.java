package com.loja.checkout.web;

import com.loja.checkout.contrato.PedidoRequest;
import com.loja.checkout.contrato.ResumoResponse;
import com.loja.checkout.dominio.CalculadoraResumo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** O endereco que o site chama para montar o resumo da compra. */
@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return calculadora.calcular(pedido);
    }
}
