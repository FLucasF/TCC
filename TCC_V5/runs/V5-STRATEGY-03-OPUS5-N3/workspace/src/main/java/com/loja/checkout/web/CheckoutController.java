package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.PedidoInformado;
import com.loja.checkout.dominio.ResumoCompra;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody PedidoInformado pedido) {
        return calculadora.calcular(pedido);
    }
}
