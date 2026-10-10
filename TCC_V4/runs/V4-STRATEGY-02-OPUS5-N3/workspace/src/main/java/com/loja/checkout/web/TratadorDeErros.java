package com.loja.checkout.web;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> recusado(CheckoutException excecao) {
        return recusar(excecao.erro());
    }

    /** Corpo que não dá nem para ler (JSON quebrado, texto onde era número) é pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> ilegivel(HttpMessageNotReadableException excecao) {
        return recusar(ErroCheckout.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusar(ErroCheckout erro) {
        return ResponseEntity.unprocessableEntity().body(ErroResponse.de(erro));
    }
}
