package com.loja.checkout.web;

import com.loja.checkout.dominio.ResumoCompra;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService servico;

    public CheckoutController(CheckoutService servico) {
        this.servico = servico;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody(required = false) ResumoRequest pedido) {
        return servico.calcular(pedido);
    }
}
