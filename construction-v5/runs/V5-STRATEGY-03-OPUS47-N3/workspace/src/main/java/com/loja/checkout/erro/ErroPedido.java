package com.loja.checkout.erro;

public class ErroPedido extends RuntimeException {
    private final String codigo;

    public ErroPedido(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
