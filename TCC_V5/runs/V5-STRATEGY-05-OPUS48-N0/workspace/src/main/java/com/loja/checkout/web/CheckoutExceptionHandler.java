package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Transforma os problemas de negócio na resposta {@code { "erro": "CODIGO" }}.
 */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    /** Pedido recusado por uma regra de negócio: devolve só o código do problema. */
    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratarNegocio(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getCodigo().name()));
    }

    /**
     * JSON malformado ou com tipo errado (ex.: texto onde devia vir número) também
     * significa pedido inválido.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
