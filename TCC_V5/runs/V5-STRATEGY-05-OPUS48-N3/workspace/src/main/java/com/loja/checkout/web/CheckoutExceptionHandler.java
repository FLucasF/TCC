package com.loja.checkout.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz as recusas em corpo {@code { "erro": "CODIGO" }}. */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> aoRecusar(CheckoutException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(ex.codigo()));
    }

    /** Corpo ausente ou malformado é um pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> aoNaoLer(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
