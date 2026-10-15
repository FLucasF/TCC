package com.loja.checkout.exception;

import com.loja.checkout.enums.CodigoErro;

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
