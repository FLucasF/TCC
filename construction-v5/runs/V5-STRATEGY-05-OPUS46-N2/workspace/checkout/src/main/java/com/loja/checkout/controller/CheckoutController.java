package com.loja.checkout.controller;

import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest pedido) {
        try {
            ResumoResponse resumo = checkoutService.calcular(pedido);
            return ResponseEntity.ok(resumo);
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.codigo()));
        }
    }
}
