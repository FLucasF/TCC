package com.loja.resumocompra.web;

import java.util.Map;

import com.loja.resumocompra.ErroPedido;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<Map<String, String>> tratar(ErroPedido erro) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("erro", erro.getCodigo().name()));
    }
}
