package com.loja.checkout.web;

import com.loja.checkout.calculo.CalculadoraResumo;
import com.loja.checkout.calculo.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Por onde o site pede o resumo da compra. */
@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest requisicao) {
        return calculadora.calcular(requisicao);
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(PedidoRecusadoException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.codigo()));
    }

    /** JSON que o site mandou fora do formato combinado: o pedido nao da para ler. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
