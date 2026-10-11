package com.loja.resumo;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(ErroCompra.class)
    public ResponseEntity<Map<String, String>> tratar(ErroCompra erro) {
        return ResponseEntity.badRequest().body(Map.of("erro", erro.codigo().name()));
    }
}
