package com.loja.resumo;

import com.loja.resumo.dominio.PedidoRecusadoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(PedidoRecusadoException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.getCodigo().name()));
    }
}
