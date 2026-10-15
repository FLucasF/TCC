package com.loja.checkout.domain;

/**
 * Pedido recusado. Carrega apenas o código do problema, que é o que o
 * serviço devolve ao site.
 */
public class PedidoRecusadoException extends RuntimeException {

    private final String codigo;

    public PedidoRecusadoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
