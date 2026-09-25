package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService servico;

    public CheckoutController(CheckoutService servico) {
        this.servico = servico;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest pedido) {
        return servico.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> erroDeNegocio(CheckoutException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.erro().name()));
    }

    /** Corpo ausente ou mal formado (ex.: quantidade que nao e um numero inteiro). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(ErroCheckout.PEDIDO_INVALIDO.name()));
    }
}
