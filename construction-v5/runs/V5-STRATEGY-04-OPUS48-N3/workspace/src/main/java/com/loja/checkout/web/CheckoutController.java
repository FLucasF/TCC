package com.loja.checkout.web;

import com.loja.checkout.domain.CalculadoraResumo;
import com.loja.checkout.domain.CheckoutException;
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
    public RespostaResumo resumo(@RequestBody RequisicaoCheckout requisicao) {
        return calculadora.calcular(requisicao);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getCodigo()));
    }
}
