package com.loja.checkout.api;

import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz a recusa do pedido na resposta com o codigo do problema. */
@RestControllerAdvice
public class PedidoRecusadoHandler {

    @ExceptionHandler(PedidoRecusado.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusado recusa) {
        return ResponseEntity.unprocessableEntity().body(ErroResponse.de(recusa.erro()));
    }

    /** Dados que nem chegam a formar um pedido (numero no lugar errado, JSON quebrado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErroResponse.de(Erro.PEDIDO_INVALIDO));
    }
}
