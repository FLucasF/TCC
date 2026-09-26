package com.loja.checkout.api;

public class ErroCheckoutException extends RuntimeException {

    private final String erro;

    public ErroCheckoutException(String erro) {
        super(erro);
        this.erro = erro;
    }

    public String erro() {
        return erro;
    }
}
