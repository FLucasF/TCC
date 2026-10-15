package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse handleCheckoutException(CheckoutException ex) {
        return new ErroResponse(ex.getCodigo());
    }
}
