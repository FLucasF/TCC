package com.loja.checkout.domain;

/** Pedido que não pode ser calculado. Carrega o código do problema a devolver. */
public class PedidoInvalidoException extends RuntimeException {

    private final String codigo;

    public PedidoInvalidoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
