package com.loja.checkout.api;

import com.loja.checkout.aplicacao.CodigoErro;
import com.loja.checkout.aplicacao.ErroDeNegocio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroDeNegocio.class)
    public ResponseEntity<ErroResponse> erroDeNegocio(ErroDeNegocio erro) {
        return ResponseEntity.badRequest().body(new ErroResponse(erro.codigo().name()));
    }

    /** Corpo malformado (ex.: preco em texto) tambem e pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
