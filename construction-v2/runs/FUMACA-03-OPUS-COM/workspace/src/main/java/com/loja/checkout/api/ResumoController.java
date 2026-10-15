package com.loja.checkout.api;

import com.loja.checkout.dominio.CalculadoraResumo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest pedido) {
        return calculadora.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> erroDeCheckout(CheckoutException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(erro.codigo().name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
