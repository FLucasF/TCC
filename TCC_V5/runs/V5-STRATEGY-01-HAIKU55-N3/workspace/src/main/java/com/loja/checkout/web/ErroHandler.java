package com.loja.checkout.web;

import com.loja.checkout.dominio.RecusaPedido;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(RecusaPedido.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse recusado(RecusaPedido recusa) {
        return new ErroResponse(recusa.erro().name());
    }
}
