package com.loja.checkout.web;

import com.loja.checkout.servico.CalculadoraResumo;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadoraResumo;

    public CheckoutController(CalculadoraResumo calculadoraResumo) {
        this.calculadoraResumo = calculadoraResumo;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return calculadoraResumo.calcular(pedido);
    }
}
