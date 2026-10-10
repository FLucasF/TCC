package com.loja.resumo.exception;

import com.loja.resumo.dto.ErroResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ErroPedidoException.class)
    public ResponseEntity<ErroResponse> tratarErroPedido(ErroPedidoException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(ex.getCodigo()));
    }
}
