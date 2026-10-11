package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte a recusa do pedido na resposta { "erro": "CODIGO" }. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> pedidoRecusado(PedidoRecusadoException excecao) {
        return recusar(excecao.getCodigo());
    }

    /** JSON malformado ou com tipo errado nos numeros: o pedido nao e valido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return recusar(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> recusar(CodigoErro codigo) {
        return ResponseEntity.badRequest().body(new ErroResponse(codigo.name()));
    }
}
