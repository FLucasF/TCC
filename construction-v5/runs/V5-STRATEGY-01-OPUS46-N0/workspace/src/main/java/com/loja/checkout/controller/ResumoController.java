package com.loja.checkout.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CheckoutService;

@RestController
public class ResumoController {

    private final CheckoutService checkoutService;

    public ResumoController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse calcularResumo(@RequestBody PedidoRequest pedido) {
        return checkoutService.calcular(pedido);
    }
}
