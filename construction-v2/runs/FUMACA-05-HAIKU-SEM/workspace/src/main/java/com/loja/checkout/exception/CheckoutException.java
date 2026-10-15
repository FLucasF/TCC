package com.loja.checkout.exception;

public class CheckoutException extends Exception {

    private final String codigoErro;

    public CheckoutException(String codigoErro, String mensagem) {
        super(mensagem);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }
}
