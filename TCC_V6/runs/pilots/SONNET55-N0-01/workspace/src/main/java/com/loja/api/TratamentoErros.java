package com.loja.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.loja.dominio.ErroNegocio;

@RestControllerAdvice
public class TratamentoErros {

    private static final Logger log = LoggerFactory.getLogger(TratamentoErros.class);

    @ExceptionHandler(ErroNegocio.class)
    ResponseEntity<Map<String, String>> negocio(ErroNegocio e) {
        return ResponseEntity.status(e.status()).body(Map.of("erro", e.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.status(422).body(Map.of("erro", "PEDIDO_INVALIDO"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> inesperado(Exception e) throws Exception {
        if (e instanceof ErrorResponse resposta) {
            return ResponseEntity.status(resposta.getStatusCode()).body(Map.of("erro", "REQUISICAO_INVALIDA"));
        }
        log.error("Erro inesperado", e);
        return ResponseEntity.status(500).body(Map.of("erro", "ERRO_INTERNO"));
    }
}
