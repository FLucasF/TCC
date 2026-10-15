package com.loja.checkout.exception;

public class PedidoException extends RuntimeException {

    private final String codigo;

    public PedidoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
