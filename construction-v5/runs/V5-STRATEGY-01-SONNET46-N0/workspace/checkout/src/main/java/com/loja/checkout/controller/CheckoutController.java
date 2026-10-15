package com.loja.checkout.controller;

import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<Object> resumo(@RequestBody PedidoRequest request) {
        Object resultado = checkoutService.calcularResumo(request);
        return ResponseEntity.ok(resultado);
    }
}
