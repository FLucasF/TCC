package com.loja.checkout.web;

import com.loja.checkout.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<RespostaErro> pedidoRecusado(ErroCheckout erro) {
        return ResponseEntity.unprocessableEntity().body(new RespostaErro(erro.codigo()));
    }

    /** Corpo que nem da para ler (numero onde era texto, JSON quebrado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> corpoIlegivel(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new RespostaErro("PEDIDO_INVALIDO"));
    }
}
