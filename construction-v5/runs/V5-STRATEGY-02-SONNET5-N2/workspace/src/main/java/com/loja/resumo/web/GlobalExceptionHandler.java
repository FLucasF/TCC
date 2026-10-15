package com.loja.resumo.web;

import com.loja.resumo.dto.ErroResponse;
import com.loja.resumo.erro.CodigoErro;
import com.loja.resumo.erro.PedidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse tratarPedidoInvalido(PedidoException e) {
        return new ErroResponse(e.getCodigo().name());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse tratarCorpoInvalido(HttpMessageNotReadableException e) {
        return new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name());
    }
}
