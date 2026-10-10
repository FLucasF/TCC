package com.loja.checkout.web;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeRecusas {

    @ExceptionHandler(PedidoRecusadoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta recusa(PedidoRecusadoException excecao) {
        return ErroResposta.de(excecao.codigo());
    }

    /** Corpo que nem da para ler (campo com tipo errado, JSON quebrado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ErroResposta.de(CodigoErro.PEDIDO_INVALIDO);
    }
}
