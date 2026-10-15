package com.loja.checkout.exception;

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
