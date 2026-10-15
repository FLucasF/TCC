package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz os problemas do pedido para a resposta { "erro": "CODIGO" }. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutInvalidoException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(CheckoutInvalidoException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.codigo().name()));
    }

    /** Corpo que nem chega a virar um pedido (JSON quebrado, campo com tipo errado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
