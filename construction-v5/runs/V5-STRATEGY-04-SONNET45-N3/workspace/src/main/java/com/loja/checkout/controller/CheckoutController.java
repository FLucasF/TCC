package com.loja.checkout.controller;

import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.service.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final ResumoService resumoService;

    public CheckoutController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody PedidoRequest request) {
        Object resultado = resumoService.calcular(request);
        return ResponseEntity.ok(resultado);
    }
}
