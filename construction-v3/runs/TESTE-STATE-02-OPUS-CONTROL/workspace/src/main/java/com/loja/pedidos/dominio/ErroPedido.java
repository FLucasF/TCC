package com.loja.pedidos.dominio;

import org.springframework.http.HttpStatus;

/** Os erros que o servico devolve para o site, com o status de cada um. */
public enum ErroPedido {

    /** Valor dos produtos zero, negativo ou ausente, ou frete negativo ou ausente. */
    PEDIDO_INVALIDO(HttpStatus.BAD_REQUEST),

    /** Nao existe pedido com o id informado. */
    PEDIDO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND),

    /** A acao pedida nao existe ou nao foi informada. */
    ACAO_INVALIDA(HttpStatus.BAD_REQUEST),

    /** A acao existe, mas nao vale na situacao atual do pedido. */
    ACAO_NAO_PERMITIDA(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErroPedido(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public ErroPedidoException excecao() {
        return new ErroPedidoException(this);
    }
}
