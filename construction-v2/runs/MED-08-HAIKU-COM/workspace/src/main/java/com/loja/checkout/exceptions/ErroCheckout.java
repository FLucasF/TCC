package com.loja.checkout.exceptions;

public class ErroCheckout extends Exception {
    private final String codigo;

    public ErroCheckout(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
