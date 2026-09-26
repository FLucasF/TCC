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
    public ResponseEntity<ErroResponse> erroDeCheckout(ErroCheckoutException excecao) {
        return resposta(excecao.erro());
    }

    /** Corpo que o site nao conseguiu montar direito entra como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return resposta(ErroCheckout.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> resposta(ErroCheckout erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(erro.name()));
    }
}
