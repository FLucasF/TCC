package com.loja.checkout.web;

import com.loja.checkout.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService servico;

    public CheckoutController(CheckoutService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return servico.calcular(pedido);
    }
}
