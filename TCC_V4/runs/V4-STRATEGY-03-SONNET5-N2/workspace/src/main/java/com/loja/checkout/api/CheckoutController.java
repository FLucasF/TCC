package com.loja.checkout.api;

import com.loja.checkout.servico.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoPedidoResponse resumo(@RequestBody ResumoPedidoRequest request) {
        return checkoutService.calcularResumo(request);
    }
}
