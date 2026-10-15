package com.loja.checkout.api;

import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Transforma as recusas do dominio na resposta { "erro": "CODIGO" }. */
@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> recusa(CheckoutException excecao) {
        return responder(excecao.getCodigo());
    }

    /** JSON malformado ou com tipos errados: nao da para calcular o pedido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return responder(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> responder(CodigoErro codigo) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(codigo.name()));
    }
}
