package com.loja.checkout.api;

import com.loja.checkout.calculo.ErroCheckout;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ErroCheckoutHandler {

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<Map<String, String>> tratar(ErroCheckout erro) {
        return ResponseEntity.badRequest().body(Map.of("erro", erro.codigo()));
    }
}
