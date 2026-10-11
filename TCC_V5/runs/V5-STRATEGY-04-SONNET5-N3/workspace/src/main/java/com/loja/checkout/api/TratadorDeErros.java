package com.loja.checkout.api;

import com.loja.checkout.CalculoCheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CalculoCheckoutException.class)
    public ResponseEntity<ErroResponse> tratar(CalculoCheckoutException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(exception.getCodigo()));
    }
}
