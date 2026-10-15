package com.loja.checkout.api;

import com.loja.checkout.api.dto.ErroResponse;
import com.loja.checkout.servico.ErroNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroNegocioHandler {

    @ExceptionHandler(ErroNegocioException.class)
    public ResponseEntity<ErroResponse> tratar(ErroNegocioException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(excecao.codigo()));
    }
}
