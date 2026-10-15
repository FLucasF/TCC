package com.loja.checkout.api;

import com.loja.checkout.api.dto.ResumoCompraRequest;
import com.loja.checkout.api.dto.ResumoCompraResponse;
import com.loja.checkout.servico.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResumoCompraResponse resumo(@RequestBody ResumoCompraRequest pedido) {
        return checkoutService.calcular(pedido);
    }
}
