package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> checkout(CheckoutException excecao) {
        return erro(excecao.getCodigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return erro(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> erro(CodigoErro codigo) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(codigo.name()));
    }
}
