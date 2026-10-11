package com.loja.resumo.api;

import com.loja.resumo.calculo.PedidoRecusadoException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<Map<String, String>> pedidoRecusado(PedidoRecusadoException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", excecao.getCodigo()));
    }
}
