package com.loja.checkout.api;

import com.loja.checkout.api.dto.ErroResponse;
import com.loja.checkout.domain.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Transforma uma recusa de pedido em { "erro": "CODIGO" }. */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratar(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getErro().name()));
    }
}
