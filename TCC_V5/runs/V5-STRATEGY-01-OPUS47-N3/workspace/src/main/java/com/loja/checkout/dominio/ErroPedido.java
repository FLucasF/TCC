package com.loja.checkout.dominio;

public final class ErroPedido extends RuntimeException {
    private final String codigo;

    public ErroPedido(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
