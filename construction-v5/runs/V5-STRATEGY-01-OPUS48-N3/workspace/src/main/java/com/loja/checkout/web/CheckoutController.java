package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.PedidoRecusadoException;
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
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return calculadora.calcular(pedido);
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(PedidoRecusadoException excecao) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(excecao.codigo().name()));
    }
}
