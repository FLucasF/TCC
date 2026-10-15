package com.loja.checkout.exception;

import com.loja.checkout.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse tratarCheckoutException(CheckoutException ex) {
        return new ErroResponse(ex.getCodigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return new ErroResponse("PEDIDO_INVALIDO");
    }
}
