package com.loja.checkout.web;

import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutInvalidoException.class)
    public ResponseEntity<ErroResposta> checkoutInvalido(CheckoutInvalidoException excecao) {
        return erro(excecao.erro());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return erro(ErroCheckout.PEDIDO_INVALIDO);
    }

    private static ResponseEntity<ErroResposta> erro(ErroCheckout erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResposta(erro.name()));
    }
}
