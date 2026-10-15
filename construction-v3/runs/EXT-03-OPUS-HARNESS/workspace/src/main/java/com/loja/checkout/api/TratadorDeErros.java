package com.loja.checkout.api;

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
    public ResponseEntity<RespostaErro> checkoutInvalido(CheckoutInvalidoException excecao) {
        return resposta(excecao.erro());
    }

    /** Corpo que o site nao conseguiu montar direito entra como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return resposta(ErroCheckout.PEDIDO_INVALIDO);
    }

    private ResponseEntity<RespostaErro> resposta(ErroCheckout erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RespostaErro(erro.name()));
    }
}
