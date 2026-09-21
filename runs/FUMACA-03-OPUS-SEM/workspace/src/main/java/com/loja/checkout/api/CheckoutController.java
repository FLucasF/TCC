package com.loja.checkout.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;

@RestController
public class CheckoutController {

    private final ResumoCheckoutService servico;

    public CheckoutController(ResumoCheckoutService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResposta resumo(@RequestBody(required = false) ResumoRequisicao requisicao) {
        if (requisicao == null) {
            throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
        }
        return servico.calcular(requisicao);
    }

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<ErroResposta> erroDeNegocio(ErroCheckout erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResposta(erro.getCodigo().name()));
    }

    /** Corpo mal formado (ex.: preco em texto) tambem e um pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResposta(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
