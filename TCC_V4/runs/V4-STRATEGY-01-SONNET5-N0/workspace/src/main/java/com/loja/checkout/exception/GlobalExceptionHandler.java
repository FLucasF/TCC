package com.loja.checkout.exception;

import com.loja.checkout.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratarCheckoutException(CheckoutException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(ex.getCodigo().name()));
    }
}
