package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Transforma os erros de negócio na resposta combinada: {@code {"erro": "CODIGO"}}.
 */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratar(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getCodigo()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        // Corpo ausente ou JSON malformado: sem itens válidos, o pedido é inválido.
        return ResponseEntity.unprocessableEntity().body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
