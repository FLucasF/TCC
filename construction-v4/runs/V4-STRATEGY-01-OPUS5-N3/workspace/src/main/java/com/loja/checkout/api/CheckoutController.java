package com.loja.checkout.api;

import com.loja.checkout.CalculoResumoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculoResumoService servico;

    public CheckoutController(CalculoResumoService servico) {
        this.servico = servico;
    }

    @PostMapping("/resumo")
    public ResumoResposta resumo(@RequestBody(required = false) ResumoRequisicao requisicao) {
        return servico.calcular(requisicao == null ? vazia() : requisicao);
    }

    private ResumoRequisicao vazia() {
        return new ResumoRequisicao(null, null, null, null, null, null, null);
    }
}
