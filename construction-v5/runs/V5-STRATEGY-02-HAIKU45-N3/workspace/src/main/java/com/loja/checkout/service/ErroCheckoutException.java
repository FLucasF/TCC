package com.loja.checkout.service;

public class ErroCheckoutException extends RuntimeException {

    private final String codigo;

    public ErroCheckoutException(String codigo) {
        super();
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

}
