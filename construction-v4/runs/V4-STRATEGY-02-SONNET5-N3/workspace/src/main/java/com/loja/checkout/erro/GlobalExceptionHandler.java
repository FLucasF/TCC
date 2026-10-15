package com.loja.checkout.erro;

import com.loja.checkout.dto.ErroResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoInvalido(PedidoException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo().name()));
    }
}
