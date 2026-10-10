package com.loja.checkout.api;

import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResposta> pedidoRecusado(PedidoRecusadoException excecao) {
        return recusar(excecao.erro());
    }

    /** JSON malformado ou com tipo errado: o pedido não dá nem para ser lido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return recusar(ErroPedido.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResposta> recusar(ErroPedido erro) {
        return ResponseEntity.badRequest().body(new ErroResposta(erro.name()));
    }
}
