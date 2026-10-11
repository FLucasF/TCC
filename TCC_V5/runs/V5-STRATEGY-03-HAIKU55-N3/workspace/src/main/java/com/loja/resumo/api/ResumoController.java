package com.loja.resumo.api;

import com.loja.resumo.calculo.ResumoCompraService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoCompraService servico;

    public ResumoController(ResumoCompraService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return servico.calcular(pedido);
    }
}
