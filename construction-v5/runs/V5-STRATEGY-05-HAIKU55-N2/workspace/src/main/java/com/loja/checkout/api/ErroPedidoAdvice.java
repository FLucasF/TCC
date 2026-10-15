package com.loja.checkout.api;

import com.loja.checkout.calculo.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ErroPedidoAdvice {

    @ExceptionHandler(PedidoRecusadoException.class)
    ResponseEntity<ErroResponse> recusar(PedidoRecusadoException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(excecao.getCodigo().name()));
    }
}
