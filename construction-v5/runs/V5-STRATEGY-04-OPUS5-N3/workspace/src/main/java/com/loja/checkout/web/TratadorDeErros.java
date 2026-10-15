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
        return recusa(excecao.codigo());
    }

    /** Corpo que o serviço não consegue nem ler é pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return recusa(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusa(CodigoErro codigo) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(codigo.name()));
    }
}
