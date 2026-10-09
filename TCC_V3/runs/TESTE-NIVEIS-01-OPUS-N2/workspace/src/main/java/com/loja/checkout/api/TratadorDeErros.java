package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse recusado(PedidoRecusadoException excecao) {
        return new ErroResponse(excecao.codigo().name());
    }

    /** Corpo que nem chega a virar um pedido (numero no lugar errado, JSON quebrado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse corpoIlegivel(HttpMessageNotReadableException excecao) {
        return new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name());
    }
}
