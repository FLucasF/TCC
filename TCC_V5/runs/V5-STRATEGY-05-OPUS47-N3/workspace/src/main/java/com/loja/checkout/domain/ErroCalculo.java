package com.loja.checkout.domain;

public class ErroCalculo extends RuntimeException {
    private final String codigo;

    public ErroCalculo(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
