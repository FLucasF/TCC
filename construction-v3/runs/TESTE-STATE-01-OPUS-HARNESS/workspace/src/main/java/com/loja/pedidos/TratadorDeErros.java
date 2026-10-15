package com.loja.pedidos;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroDoPedido.class)
    public ResponseEntity<ErroResponse> erroDoPedido(ErroDoPedido erro) {
        return resposta(erro.getCodigo());
    }

    /** Corpo que o Jackson não consegue ler: vale como dado ausente do respectivo pedido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpServletRequest requisicao) {
        return resposta(requisicao.getRequestURI().endsWith("/acoes")
                ? ErroDoPedido.Codigo.ACAO_INVALIDA
                : ErroDoPedido.Codigo.PEDIDO_INVALIDO);
    }

    private ResponseEntity<ErroResponse> resposta(ErroDoPedido.Codigo codigo) {
        return ResponseEntity.status(codigo.getStatus()).body(new ErroResponse(codigo.name()));
    }
}
