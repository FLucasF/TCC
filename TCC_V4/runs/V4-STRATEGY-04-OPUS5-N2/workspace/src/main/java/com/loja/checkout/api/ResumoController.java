package com.loja.checkout.api;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.PedidoRecebido;
import com.loja.checkout.dominio.ResumoCompra;
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
    public ResumoCompra resumo(@RequestBody PedidoRecebido pedido) {
        return calculadora.calcular(pedido);
    }
}
