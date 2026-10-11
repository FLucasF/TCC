package com.loja.resumo.web;

import com.loja.resumo.service.ErroPedidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroPedidoException.class)
    public ResponseEntity<ErroResponse> tratar(ErroPedidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(ex.getCodigo()));
    }
}
