package com.loja.checkout.web;

import com.loja.checkout.aplicacao.CodigoErro;
import com.loja.checkout.aplicacao.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PedidoRecusadoHandler {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return recusar(excecao.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return recusar(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusar(CodigoErro codigo) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroResponse(codigo.name()));
    }
}
