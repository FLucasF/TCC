package com.loja.checkout.web;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> recusado(CheckoutException e) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(e.codigo().name()));
    }

    /** JSON ilegivel ou mal formado conta como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
