package com.loja.checkout.api;

import com.loja.checkout.CheckoutException;
import com.loja.checkout.calculo.CalculadoraCheckout;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraCheckout calculadora;

    public CheckoutController(CalculadoraCheckout calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest req) {
        try {
            return ResponseEntity.ok(calculadora.calcular(req));
        } catch (CheckoutException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro", e.getCodigoErro()));
        }
    }
}
