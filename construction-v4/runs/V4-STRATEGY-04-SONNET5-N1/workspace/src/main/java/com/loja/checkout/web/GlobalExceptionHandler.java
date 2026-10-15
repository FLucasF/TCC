package com.loja.checkout.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoInvalido(PedidoException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(excecao.getCodigo()));
    }
}
