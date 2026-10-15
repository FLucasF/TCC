package com.loja.checkout.exception;

import com.loja.checkout.dto.ErroResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoException(PedidoException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(ex.getCodigo()));
    }
}
