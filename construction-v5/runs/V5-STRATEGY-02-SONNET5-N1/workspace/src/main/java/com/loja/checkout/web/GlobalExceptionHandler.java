package com.loja.checkout.web;

import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.web.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoInvalido(PedidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(ex.getCodigo()));
    }
}
