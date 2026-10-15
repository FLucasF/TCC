package com.loja.checkout.web;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> checkout(CheckoutException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.erro().name()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(ErroCheckout.PEDIDO_INVALIDO.name()));
    }
}
