package com.loja.checkout.exception;

public class CheckoutException extends RuntimeException {

    private final CodigoErro codigoErro;

    public CheckoutException(CodigoErro codigoErro) {
        super(codigoErro.name());
        this.codigoErro = codigoErro;
    }

    public CodigoErro getCodigoErro() {
        return codigoErro;
    }
}
