package com.loja.pedidos.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(PedidoInvalidoException.class)
    public ResponseEntity<ErroResposta> tratarPedidoInvalido() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResposta("PEDIDO_INVALIDO"));
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarPedidoNaoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResposta("PEDIDO_NAO_ENCONTRADO"));
    }

    @ExceptionHandler(AcaoInvalidaException.class)
    public ResponseEntity<ErroResposta> tratarAcaoInvalida() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResposta("ACAO_INVALIDA"));
    }

    @ExceptionHandler(AcaoNaoPermitidaException.class)
    public ResponseEntity<ErroResposta> tratarAcaoNaoPermitida() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResposta("ACAO_NAO_PERMITIDA"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoInvalido() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResposta("PEDIDO_INVALIDO"));
    }
}
