package com.loja.checkout.controller;

import com.loja.checkout.dto.ErrorResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCheckoutException(CheckoutException ex) {
        return new ErrorResponse(ex.getCodigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMalformedRequest(HttpMessageNotReadableException ex) {
        return new ErrorResponse("PEDIDO_INVALIDO");
    }
}
