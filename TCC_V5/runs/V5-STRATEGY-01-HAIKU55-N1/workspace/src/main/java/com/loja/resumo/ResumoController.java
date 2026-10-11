package com.loja.resumo;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoService servico;

    public ResumoController(ResumoService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResposta resumo(@RequestBody ResumoRequisicao requisicao) {
        return servico.calcular(requisicao);
    }
}
