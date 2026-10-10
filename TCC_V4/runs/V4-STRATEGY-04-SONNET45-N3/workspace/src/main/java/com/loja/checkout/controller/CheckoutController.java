package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CalculadoraResumo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody ResumoRequest request) {
        try {
            ResumoResponse response = calculadora.calcular(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo()));
        }
    }
}
