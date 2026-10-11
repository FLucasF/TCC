package com.loja.checkout.web;

import com.loja.checkout.calculo.ResumoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ResumoException.class)
    public ResponseEntity<ErroResponse> tratar(ResumoException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.erro().name()));
    }
}
