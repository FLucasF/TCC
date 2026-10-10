package com.loja.checkout.api;

import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Pedido recusado: devolve so o codigo do problema. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResposta> recusado(PedidoRecusadoException excecao) {
        return recusar(excecao.erro());
    }

    /** Corpo que nem chega a ser lido como um pedido tambem e pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> ilegivel(HttpMessageNotReadableException excecao) {
        return recusar(Erro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResposta> recusar(Erro erro) {
        return ResponseEntity.badRequest().body(new ErroResposta(erro.name()));
    }
}
