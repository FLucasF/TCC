package com.loja.checkout.exception;

public class ResumoCheckoutException extends RuntimeException {
    private final String codigoErro;

    public ResumoCheckoutException(String codigoErro) {
        super("Erro no checkout: " + codigoErro);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }
}
