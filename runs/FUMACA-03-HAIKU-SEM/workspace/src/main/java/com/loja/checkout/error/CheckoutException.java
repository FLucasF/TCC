package com.loja.checkout.error;

public class CheckoutException extends RuntimeException {

    private final String codigoErro;

    public CheckoutException(String codigoErro) {
        super("Erro no checkout: " + codigoErro);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }
}
