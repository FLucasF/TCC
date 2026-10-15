package com.loja.checkout.web;

import com.loja.checkout.dominio.MotivoRecusa;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeRecusas {

    @ExceptionHandler(PedidoRecusadoException.class)
    ResponseEntity<RespostaDeErro> recusa(PedidoRecusadoException excecao) {
        return recusar(excecao.motivo());
    }

    /** JSON que nao da nem para ler (ex.: preco em texto) e pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<RespostaDeErro> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return recusar(MotivoRecusa.PEDIDO_INVALIDO);
    }

    private ResponseEntity<RespostaDeErro> recusar(MotivoRecusa motivo) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespostaDeErro(motivo.name()));
    }
}
