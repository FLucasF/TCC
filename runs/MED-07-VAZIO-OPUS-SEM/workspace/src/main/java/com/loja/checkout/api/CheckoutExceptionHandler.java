package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz os erros de negocio para 400 com { "erro": "CODIGO" }. */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> negocio(CheckoutException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.getCodigo().name()));
    }

    /** Corpo ausente ou mal formado: tratado como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
