package com.loja.checkout.web;

import com.loja.checkout.dominio.ErroPedido;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<ErroResponse> erroPedido(ErroPedido erro) {
        return recusa(erro.codigo());
    }

    /** JSON que o site não conseguiu montar direito entra como pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonIlegivel(HttpMessageNotReadableException erro) {
        return recusa("PEDIDO_INVALIDO");
    }

    private ResponseEntity<ErroResponse> recusa(String codigo) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(codigo));
    }
}
