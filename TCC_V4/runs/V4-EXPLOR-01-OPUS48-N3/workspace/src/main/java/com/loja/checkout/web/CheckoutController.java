package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.Resumo;
import com.loja.checkout.service.CalculadoraResumo;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody ResumoRequest req) {
        return calculadora.calcular(req);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> recusar(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(Map.of("erro", ex.getCodigo()));
    }
}
