package com.loja.checkout.api;

import com.loja.checkout.CalculadoraResumo;
import com.loja.checkout.CheckoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CheckoutController {

    private final CalculadoraResumo calculadora;

    CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return calculadora.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    ResponseEntity<ErroResponse> recusado(CheckoutException e) {
        return ResponseEntity.unprocessableContent().body(new ErroResponse(e.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.unprocessableContent().body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
