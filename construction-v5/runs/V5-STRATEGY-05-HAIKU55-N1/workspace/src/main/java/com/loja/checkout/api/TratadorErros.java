package com.loja.checkout.api;

import com.loja.checkout.dominio.ErroPedido;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<Map<String, String>> tratar(ErroPedido erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", erro.getCodigo().name()));
    }
}
