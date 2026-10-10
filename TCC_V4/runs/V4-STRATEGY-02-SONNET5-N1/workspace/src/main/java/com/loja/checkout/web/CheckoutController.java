package com.loja.checkout.web;

import com.loja.checkout.servico.ResumoCompraService;
import com.loja.checkout.web.dto.ResumoCompraRequest;
import com.loja.checkout.web.dto.ResumoCompraResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoCompraService resumoCompraService;

    public CheckoutController(ResumoCompraService resumoCompraService) {
        this.resumoCompraService = resumoCompraService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompraResponse resumo(@RequestBody ResumoCompraRequest requisicao) {
        return resumoCompraService.calcular(requisicao);
    }
}
