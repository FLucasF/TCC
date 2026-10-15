package com.loja.checkout.model;

public class CheckoutException extends RuntimeException {
    private final String codigo;

    public CheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
