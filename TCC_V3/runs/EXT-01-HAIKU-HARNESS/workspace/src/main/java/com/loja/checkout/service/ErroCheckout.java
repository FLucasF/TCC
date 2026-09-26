package com.loja.checkout.service;

public class ErroCheckout extends RuntimeException {
    private final String codigo;

    public ErroCheckout(String codigo) {
        super("Erro de checkout: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
