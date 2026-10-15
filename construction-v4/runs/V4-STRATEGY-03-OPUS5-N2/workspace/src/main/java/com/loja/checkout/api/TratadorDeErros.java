package com.loja.checkout.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Devolve o pedido recusado no formato { "erro": "CODIGO" }. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return recusar(excecao.codigo());
    }

    /** Corpo que nem da para ler como pedido tambem e pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return recusar(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusar(CodigoErro codigo) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(codigo));
    }
}
