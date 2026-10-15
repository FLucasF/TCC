package com.loja.checkout.servico;

public class ErroCheckoutException extends RuntimeException {

    private final String codigoErro;

    public ErroCheckoutException(String codigoErro) {
        super(codigoErro);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }
}
