package com.loja.pedidos.erro;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(ErroNegocio.class)
    ResponseEntity<Map<String, String>> negocio(ErroNegocio e) {
        return ResponseEntity.status(e.codigo().status()).body(Map.of("erro", e.codigo().codigoPublico()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.status(422).body(Map.of("erro", CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
