package com.loja.checkout;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(ResumoException.class)
    public ResponseEntity<ErroResponse> tratar(ResumoException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(erro.codigo()));
    }
}
