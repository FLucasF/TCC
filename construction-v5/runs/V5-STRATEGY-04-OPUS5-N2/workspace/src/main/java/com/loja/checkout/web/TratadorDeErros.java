package com.loja.checkout.web;

import com.loja.checkout.aplicacao.ErroCheckout;
import com.loja.checkout.aplicacao.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<RespostaErro> pedidoRecusado(PedidoRecusadoException excecao) {
        return recusa(excecao.erro());
    }

    /** Carrinho que nem chega a ser lido (campo com formato errado) tambem e pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return recusa(ErroCheckout.PEDIDO_INVALIDO);
    }

    private ResponseEntity<RespostaErro> recusa(ErroCheckout erro) {
        return ResponseEntity.badRequest()
                .body(new RespostaErro(erro.name()));
    }
}
