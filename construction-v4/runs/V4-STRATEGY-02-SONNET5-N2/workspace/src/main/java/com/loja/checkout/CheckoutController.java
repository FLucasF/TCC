package com.loja.checkout;

import com.loja.checkout.dto.ResumoPedidoRequest;
import com.loja.checkout.dto.ResumoPedidoResponse;
import com.loja.checkout.service.CheckoutService;
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
