package com.loja.checkout.api;

import com.loja.checkout.erro.ErroNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErroNegocioException.class)
    public ResponseEntity<ErroResponse> tratar(ErroNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(ex.getCodigo()));
    }
}
