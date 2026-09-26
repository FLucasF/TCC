package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroNegocio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManipuladorErros {

    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<RespostaErro> erroNegocio(ErroNegocio erro) {
        return ResponseEntity.badRequest().body(new RespostaErro(erro.codigo().name()));
    }

    /** Corpo ausente ou com valor que nao da para ler (ex.: preco como texto). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> corpoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespostaErro(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
