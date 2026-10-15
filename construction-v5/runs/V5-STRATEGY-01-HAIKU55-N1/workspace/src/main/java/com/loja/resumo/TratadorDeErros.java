package com.loja.resumo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(ErroResumo.class)
    ResponseEntity<ErroResposta> tratar(ErroResumo erro) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErroResposta(erro.getCodigo()));
    }
}
