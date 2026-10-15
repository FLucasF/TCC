package com.loja.pedidos.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ErroDoPedido.class)
    public ResponseEntity<RespostaDeErro> tratar(ErroDoPedido excecao) {
        return responder(excecao.erro());
    }

    /** Corpo ausente ou ilegivel: e o proprio dado da requisicao que esta invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaDeErro> tratar(HttpServletRequest requisicao) {
        return responder(requisicao.getRequestURI().endsWith("/acoes") ? Erro.ACAO_INVALIDA : Erro.PEDIDO_INVALIDO);
    }

    private ResponseEntity<RespostaDeErro> responder(Erro erro) {
        return ResponseEntity.status(erro.status()).body(RespostaDeErro.de(erro));
    }
}
