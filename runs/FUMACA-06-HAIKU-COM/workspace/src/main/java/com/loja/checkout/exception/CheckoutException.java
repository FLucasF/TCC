package com.loja.checkout.exception;

public class CheckoutException extends RuntimeException {
    private final String codigo;

    public CheckoutException(String codigo) {
        super("Erro no checkout: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
