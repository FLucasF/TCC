package com.loja.resumo.api;

import com.loja.resumo.calculo.ResumoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(ResumoException.class)
    public ResponseEntity<ErroResponse> trata(ResumoException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(e.getCodigo()));
    }
}
