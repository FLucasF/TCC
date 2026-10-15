package com.loja.checkout.api;

import com.loja.checkout.api.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ErroPedidoException.class)
    public ResponseEntity<ErroResponse> tratarErroPedido(ErroPedidoException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(e.getCodigo()));
    }
}
