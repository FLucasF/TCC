package com.loja.checkout.web;

import com.loja.checkout.domain.PedidoInvalidoException;
import com.loja.checkout.service.CalculadoraResumo;
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
    public ResumoResponse resumo(@RequestBody CheckoutRequest req) {
        return calculadora.calcular(req);
    }

    /** Pedido recusado: devolve só o código do problema. */
    @ExceptionHandler(PedidoInvalidoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoInvalidoException e) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(e.getCodigo()));
    }
}
