package com.loja.checkout.exception;

public class CheckoutException extends RuntimeException {

    private final String erro;

    public CheckoutException(String erro) {
        super(erro);
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }
}
