package com.loja.checkout.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public org.springframework.http.ResponseEntity<ErroResponse> tratarCheckoutException(CheckoutException ex) {
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(ex.getCodigo().name()));
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErroResponse> tratarErroGenerico(Exception ex) {
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
