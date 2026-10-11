package com.loja.checkout.api;

import com.loja.checkout.servico.erro.PedidoInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PedidoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratar(PedidoInvalidoException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(excecao.codigo().name()));
    }
}
