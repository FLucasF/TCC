package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CalculadoraResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    private final CalculadoraResumoService calculadoraResumoService;

    public CheckoutController(CalculadoraResumoService calculadoraResumoService) {
        this.calculadoraResumoService = calculadoraResumoService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody PedidoRequest pedido) {
        try {
            ResumoResponse resumo = calculadoraResumoService.calcular(pedido);
            return ResponseEntity.ok(resumo);
        } catch (CheckoutException e) {
            ErroResponse erro = new ErroResponse(e.getCodigo().name());
            return ResponseEntity.badRequest().body(erro);
        }
    }
}
