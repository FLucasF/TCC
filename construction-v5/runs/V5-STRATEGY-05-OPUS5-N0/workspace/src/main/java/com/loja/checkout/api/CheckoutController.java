package com.loja.checkout.api;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.CheckoutResponse;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.MediaType;
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

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CheckoutResponse resumo(@RequestBody(required = false) CheckoutRequest requisicao) {
        return servico.calcular(requisicao);
    }
}
