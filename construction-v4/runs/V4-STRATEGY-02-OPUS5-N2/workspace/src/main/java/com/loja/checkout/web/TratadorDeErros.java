package com.loja.checkout.web;

import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return responder(excecao.erro());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return responder(Erro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> responder(Erro erro) {
        return ResponseEntity.badRequest().body(new ErroResponse(erro.name()));
    }
}
