package com.loja.pedidos.exception;

public class PedidoException extends Exception {
    private final String codigo;
    private final int statusCode;

    public PedidoException(String codigo, int statusCode) {
        super();
        this.codigo = codigo;
        this.statusCode = statusCode;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
