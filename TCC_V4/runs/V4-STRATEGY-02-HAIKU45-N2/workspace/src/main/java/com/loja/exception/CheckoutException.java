package com.loja.exception;

public class CheckoutException extends RuntimeException {
    private final String codigoErro;

    public CheckoutException(String codigoErro) {
        super();
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }
}
