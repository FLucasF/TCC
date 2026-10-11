package com.loja.checkout.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoService resumoService;

    public CheckoutController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping(path = "/checkout/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoResponse resumo(@RequestBody ResumoRequest requisicao) {
        return resumoService.calcular(requisicao);
    }
}
