package com.loja.checkout.api;

public class ErroPedidoException extends RuntimeException {

    private final String codigo;

    public ErroPedidoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
