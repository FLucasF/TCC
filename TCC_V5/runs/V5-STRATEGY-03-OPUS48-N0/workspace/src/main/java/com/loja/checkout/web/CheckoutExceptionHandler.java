package com.loja.checkout.web;

import com.loja.checkout.service.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Transforma as recusas de pedido na resposta {@code { "erro": "CODIGO" }}.
 */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    /** Pedido recusado por uma regra de negocio: devolve o codigo do problema. */
    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratarRegra(CheckoutException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(ex.getCodigo().name()));
    }

    /**
     * JSON ausente ou malformado: tratamos como pedido invalido, ja que nao da
     * nem para montar o carrinho.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
