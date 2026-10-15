package com.loja.service;

public class ErroCheckout extends RuntimeException {

    private final String codigo;

    public ErroCheckout(String codigo) {
        super("Erro: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

}
