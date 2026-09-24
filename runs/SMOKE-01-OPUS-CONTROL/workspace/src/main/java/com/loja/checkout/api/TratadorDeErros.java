package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte os erros de negocio no formato {"erro": "CODIGO"} com status 400. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> negocio(CheckoutException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.getErro().name()));
    }

    /** Corpo ausente ou mal formado: o carrinho nao pode ser lido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(ErroCheckout.PEDIDO_INVALIDO.name()));
    }
}
