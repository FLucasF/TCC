package com.loja.pedidos.api;

import org.springframework.http.HttpStatus;

public enum Erro {

    PEDIDO_INVALIDO(HttpStatus.BAD_REQUEST),
    PEDIDO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND),
    ACAO_INVALIDA(HttpStatus.BAD_REQUEST),
    ACAO_NAO_PERMITIDA(HttpStatus.CONFLICT);

    private final HttpStatus status;

    Erro(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public ErroDoPedido excecao() {
        return new ErroDoPedido(this);
    }
}
