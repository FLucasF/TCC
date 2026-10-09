package com.loja.checkout.web;

import com.loja.checkout.calculo.CodigoErro;
import com.loja.checkout.calculo.ErroPedido;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<Map<String, String>> erroDePedido(ErroPedido e) {
        return resposta(e.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(CodigoErro.PEDIDO_INVALIDO);
    }

    private static ResponseEntity<Map<String, String>> resposta(CodigoErro codigo) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", codigo.name()));
    }
}
