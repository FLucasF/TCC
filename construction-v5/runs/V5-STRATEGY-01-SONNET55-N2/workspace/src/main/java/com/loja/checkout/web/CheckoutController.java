package com.loja.checkout.web;

import com.loja.checkout.Resumo;
import com.loja.checkout.ResumoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoService service;

    public CheckoutController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody ResumoRequest request) {
        return service.calcular(request.paraPedido());
    }
}
