package com.loja.checkout.api;

import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz a recusa do pedido na resposta combinada com o site. */
@RestControllerAdvice
public class PedidosRecusados {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(excecao.erro().name()));
    }

    /** Dados que nem chegam a formar um pedido (numero onde devia vir numero, por exemplo). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(ErroPedido.PEDIDO_INVALIDO.name()));
    }
}
