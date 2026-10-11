package com.loja.checkout.web;

import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ErroCheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroCheckoutException.class)
    public ResponseEntity<RespostaErro> recusado(ErroCheckoutException excecao) {
        return recusa(excecao.erro());
    }

    /** Corpo que o site mandou mal formado é pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return recusa(ErroCheckout.PEDIDO_INVALIDO);
    }

    private ResponseEntity<RespostaErro> recusa(ErroCheckout erro) {
        return ResponseEntity.unprocessableEntity().body(new RespostaErro(erro.name()));
    }
}
