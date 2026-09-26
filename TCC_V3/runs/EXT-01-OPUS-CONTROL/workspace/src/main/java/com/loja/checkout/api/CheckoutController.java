package com.loja.checkout.api;

import com.loja.checkout.aplicacao.ResumoCheckoutService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCheckoutService service;

    public CheckoutController(ResumoCheckoutService service) {
        this.service = service;
    }

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest request) {
        return service.calcular(request);
    }
}
