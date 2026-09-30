package com.loja.pedidos.web;

import org.springframework.http.HttpStatus;

/** Os erros que o serviço devolve, cada um com o seu status. */
public enum CodigoDeErro {

    PEDIDO_INVALIDO(HttpStatus.BAD_REQUEST),
    PEDIDO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND),
    ACAO_INVALIDA(HttpStatus.BAD_REQUEST),
    ACAO_NAO_PERMITIDA(HttpStatus.CONFLICT);

    private final HttpStatus status;

    CodigoDeErro(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
