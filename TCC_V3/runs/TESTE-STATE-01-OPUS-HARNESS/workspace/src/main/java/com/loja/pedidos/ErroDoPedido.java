package com.loja.pedidos;

import org.springframework.http.HttpStatus;

public class ErroDoPedido extends RuntimeException {

    public enum Codigo {
        PEDIDO_INVALIDO(HttpStatus.BAD_REQUEST),
        PEDIDO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND),
        ACAO_INVALIDA(HttpStatus.BAD_REQUEST),
        ACAO_NAO_PERMITIDA(HttpStatus.CONFLICT);

        private final HttpStatus status;

        Codigo(HttpStatus status) {
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    private final Codigo codigo;

    public ErroDoPedido(Codigo codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public Codigo getCodigo() {
        return codigo;
    }
}
