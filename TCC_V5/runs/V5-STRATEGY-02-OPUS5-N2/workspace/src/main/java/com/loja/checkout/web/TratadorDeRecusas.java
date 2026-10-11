package com.loja.checkout.web;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeRecusas {

    @ExceptionHandler(PedidoRecusado.class)
    public ResponseEntity<ErroResposta> recusado(PedidoRecusado recusa) {
        return resposta(recusa.codigo());
    }

    /** Pedido que não dá nem para ler: o problema está nos dados da compra. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> ilegivel(HttpMessageNotReadableException ignorada) {
        return resposta(CodigoErro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResposta> resposta(CodigoErro codigo) {
        return ResponseEntity.unprocessableEntity().body(new ErroResposta(codigo.name()));
    }
}
