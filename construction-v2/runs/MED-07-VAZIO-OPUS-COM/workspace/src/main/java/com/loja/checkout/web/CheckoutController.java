package com.loja.checkout.web;

import com.loja.checkout.CheckoutService;
import com.loja.checkout.dominio.ResumoCompra;
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
    public ResumoCompra resumo(@RequestBody(required = false) ResumoRequest requisicao) {
        return servico.calcular(requisicao == null ? new ResumoRequest(null, null, null, null, null) : requisicao);
    }
}
