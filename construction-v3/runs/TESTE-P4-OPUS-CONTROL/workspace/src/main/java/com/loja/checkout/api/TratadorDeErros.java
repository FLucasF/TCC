package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<ErroResponse> erroDeNegocio(ErroCheckout erro) {
        return ResponseEntity.badRequest().body(new ErroResponse(erro.codigo().name()));
    }

    /** Corpo ausente ou com tipos que nem chegam a ser lidos: pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
