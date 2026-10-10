package com.loja.controller;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ErrorResponse;
import com.loja.service.CalculadoraCheckout;
import com.loja.service.Validador;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class CheckoutController {
    private final Validador validador = new Validador();
    private final CalculadoraCheckout calculadora = new CalculadoraCheckout();

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest request) {
        Optional<String> erro = validador.validar(request);

        if (erro.isPresent()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(erro.get()));
        }

        CheckoutResponse response = calculadora.calcular(request);
        return ResponseEntity.ok(response);
    }
}
