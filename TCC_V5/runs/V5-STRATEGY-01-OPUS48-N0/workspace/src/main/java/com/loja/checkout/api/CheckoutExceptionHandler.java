package com.loja.checkout.api;

import com.loja.checkout.service.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Transforma a recusa do pedido na resposta de erro combinada com o site:
 * apenas o codigo do problema, no corpo { "erro": "CODIGO" }.
 */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratarRecusa(CheckoutException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(ex.getCodigo().name()));
    }
}
