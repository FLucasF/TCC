package com.loja.checkout.web;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return recusa(excecao.getCodigo());
    }

    /** Corpo que nem chega a virar um pedido legível entra como pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return recusa(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusa(CodigoErro codigo) {
        return ResponseEntity.badRequest().body(new ErroResponse(codigo.name()));
    }
}
