package com.loja.checkout.web;

import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.web.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse tratarPedidoRecusado(PedidoRecusadoException excecao) {
        return new ErroResponse(excecao.codigo().name());
    }
}
