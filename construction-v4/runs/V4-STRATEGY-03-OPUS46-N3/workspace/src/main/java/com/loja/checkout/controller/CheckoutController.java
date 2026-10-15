package com.loja.checkout.controller;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.ErroResponse;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest pedido) {
        try {
            return ResponseEntity.ok(service.calcular(pedido));
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity().body(new ErroResponse(e.getCodigo()));
        }
    }
}
