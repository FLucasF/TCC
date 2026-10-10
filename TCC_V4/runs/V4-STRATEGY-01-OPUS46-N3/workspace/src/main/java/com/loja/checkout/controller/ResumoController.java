package com.loja.checkout.controller;

import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResumoController {

    private final CheckoutService service;

    public ResumoController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse calcular(@RequestBody PedidoRequest pedido) {
        return service.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handleCheckout(CheckoutException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getCodigo()));
    }
}
