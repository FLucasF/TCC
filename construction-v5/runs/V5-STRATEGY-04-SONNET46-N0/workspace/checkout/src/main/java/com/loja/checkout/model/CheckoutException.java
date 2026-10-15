package com.loja.checkout.model;

public class CheckoutException extends RuntimeException {
    private final CheckoutErro erro;

    public CheckoutException(CheckoutErro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public CheckoutErro getErro() {
        return erro;
    }
}
