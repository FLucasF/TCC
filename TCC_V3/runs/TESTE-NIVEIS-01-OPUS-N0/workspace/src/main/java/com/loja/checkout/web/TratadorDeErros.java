package com.loja.checkout.web;

import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Transforma as recusas de calculo na resposta { "erro": "CODIGO" }. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResposta> recusa(CheckoutException excecao) {
        return responder(excecao.getCodigo());
    }

    /** JSON malformado, corpo ausente ou campo com tipo errado: o pedido nao da para ler. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return responder(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResposta> responder(CodigoErro codigo) {
        return ResponseEntity.badRequest()
                .body(new ErroResposta(codigo.name()));
    }
}
