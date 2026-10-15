package com.loja.checkout.api;

import com.loja.checkout.servico.ResumoCompraServico;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCompraServico resumoCompraServico;

    public CheckoutController(ResumoCompraServico resumoCompraServico) {
        this.resumoCompraServico = resumoCompraServico;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return resumoCompraServico.calcular(request);
    }
}
