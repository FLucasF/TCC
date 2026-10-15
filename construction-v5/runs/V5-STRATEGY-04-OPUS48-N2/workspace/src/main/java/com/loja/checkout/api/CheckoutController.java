package com.loja.checkout.api;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.CheckoutResponse;
import com.loja.checkout.domain.ResumoCheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoCheckoutService service;

    public CheckoutController(ResumoCheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public CheckoutResponse resumo(@RequestBody CheckoutRequest request) {
        return service.resumir(request);
    }
}
