package com.loja.checkout.web;

import com.loja.checkout.dominio.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> aoRecusar(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getCodigo()));
    }
}
