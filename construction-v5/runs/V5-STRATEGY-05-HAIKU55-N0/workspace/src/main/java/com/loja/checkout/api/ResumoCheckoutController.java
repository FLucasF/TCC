package com.loja.checkout.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loja.checkout.calculo.CalculadoraResumo;
import com.loja.checkout.calculo.ResumoCompra;

@RestController
@RequestMapping("/checkout")
public class ResumoCheckoutController {

    private final MontadorPedido montadorPedido;
    private final CalculadoraResumo calculadoraResumo;

    public ResumoCheckoutController(MontadorPedido montadorPedido, CalculadoraResumo calculadoraResumo) {
        this.montadorPedido = montadorPedido;
        this.calculadoraResumo = calculadoraResumo;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody PedidoRequest request) {
        return calculadoraResumo.calcular(montadorPedido.montar(request));
    }
}
