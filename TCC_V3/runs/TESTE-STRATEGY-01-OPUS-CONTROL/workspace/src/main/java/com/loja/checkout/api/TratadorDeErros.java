package com.loja.checkout.api;

import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> erroDeNegocio(CheckoutException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.getCodigo().name()));
    }

    /** Corpo ausente ou com campo em formato invalido: tratamos como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
