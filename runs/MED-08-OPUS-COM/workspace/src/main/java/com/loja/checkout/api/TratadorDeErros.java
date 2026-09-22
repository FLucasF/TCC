package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroDeCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroDeCheckout.class)
    public ResponseEntity<ErroResponse> erroDeCheckout(ErroDeCheckout erro) {
        return erro(erro.codigo());
    }

    /** Corpo que o site nao conseguiu montar direito entra como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException erro) {
        return erro(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> erro(CodigoErro codigo) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(codigo.name()));
    }
}
