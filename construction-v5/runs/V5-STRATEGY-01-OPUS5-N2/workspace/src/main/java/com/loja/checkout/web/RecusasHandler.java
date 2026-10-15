package com.loja.checkout.web;

import com.loja.checkout.resumo.CodigoErro;
import com.loja.checkout.resumo.PedidoRecusadoException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RecusasHandler {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return recusa(excecao.codigo());
    }

    /** Corpo que o serviço não consegue ler (campo com formato errado, por exemplo). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return recusa(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusa(CodigoErro codigo) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(codigo.name()));
    }
}
