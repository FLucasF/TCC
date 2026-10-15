package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Toda recusa volta no mesmo formato: { "erro": "CODIGO" }. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> recusa(CheckoutException excecao) {
        return recusa(excecao.getErro());
    }

    /**
     * Corpo que nao da para ler (ex.: quantidade com texto no lugar de numero)
     * cai na primeira conferencia da lista: o pedido e invalido.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException excecao) {
        return recusa(ErroCheckout.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusa(ErroCheckout erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(erro.name()));
    }
}
