package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManipuladorDeErros {

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<ErroResponse> erroDeCheckout(ErroCheckout erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErroResponse.de(erro.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(CodigoErro.PEDIDO_INVALIDO));
    }
}
