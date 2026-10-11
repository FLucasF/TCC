package com.loja.checkout.web;

import com.loja.checkout.dominio.ErroPedidoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(ErroPedidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse tratar(ErroPedidoException excecao) {
        return new ErroResponse(excecao.getCodigo());
    }
}
