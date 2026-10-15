package com.loja.checkout.controller;

import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResumoController {

    private final ResumoService service;

    public ResumoController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody PedidoRequest pedido) {
        try {
            return ResponseEntity.ok(service.calcular(pedido));
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("erro", e.getCodigo()));
        }
    }
}
