package com.loja.checkout.web;

import com.loja.checkout.servico.ResumoCompraService;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoCompraService resumoCompraService;

    public ResumoController(ResumoCompraService resumoCompraService) {
        this.resumoCompraService = resumoCompraService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse calcularResumo(@RequestBody ResumoRequest request) {
        return resumoCompraService.calcular(request);
    }
}
