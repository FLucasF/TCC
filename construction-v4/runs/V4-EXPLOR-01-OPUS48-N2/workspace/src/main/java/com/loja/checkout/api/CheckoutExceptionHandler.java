package com.loja.checkout.api;

import com.loja.checkout.domain.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Quando o pedido é recusado, devolve só o código do problema. */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> aoRecusar(CheckoutException e) {
        return ResponseEntity.unprocessableEntity()
                .body(new ErroResponse(e.getCodigo().name()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> aoFalharNaLeitura(Exception e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
