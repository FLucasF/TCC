package com.loja.checkout.web;

import com.loja.checkout.aplicacao.CodigoErro;
import com.loja.checkout.aplicacao.PedidoRecusadoException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Pedido recusado sai sempre do mesmo jeito: o codigo do problema, e nada mais. */
@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return recusa(excecao.codigo());
    }

    /** Dados da compra que nem chegam a ser lidos sao pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return recusa(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusa(CodigoErro codigo) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(codigo.name()));
    }
}
