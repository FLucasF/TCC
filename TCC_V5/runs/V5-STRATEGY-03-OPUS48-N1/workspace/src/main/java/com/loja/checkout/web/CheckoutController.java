package com.loja.checkout.web;

import com.loja.checkout.aplicacao.ResumoCompraService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoCompraService servico;

    public CheckoutController(ResumoCompraService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompraResponse resumo(@RequestBody ResumoCompraRequest req) {
        return servico.calcular(req);
    }
}
