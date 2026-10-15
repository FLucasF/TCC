package com.loja.checkout.controller;

import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoService resumoService;

    public ResumoController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<Object> calcularResumo(@RequestBody PedidoRequest pedido) {
        Object resultado = resumoService.calcular(pedido);
        if (resultado instanceof ResumoResponse) {
            return ResponseEntity.ok(resultado);
        }
        return ResponseEntity.unprocessableEntity().body(resultado);
    }
}
