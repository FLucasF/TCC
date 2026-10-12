package com.loja.checkout.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;

@RestControllerAdvice
public class ErroCheckoutHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> tratarErroCheckout(CheckoutException e) {
        return responder(e.getErro());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> tratarJsonInvalido(HttpMessageNotReadableException e) {
        return responder(ErroCheckout.PEDIDO_INVALIDO);
    }

    private ResponseEntity<Map<String, String>> responder(ErroCheckout erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", erro.name()));
    }
}
