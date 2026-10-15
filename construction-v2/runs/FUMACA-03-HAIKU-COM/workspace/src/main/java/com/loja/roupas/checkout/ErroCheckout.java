package com.loja.roupas.checkout;

public class ErroCheckout extends RuntimeException {
    private final String codigoErro;

    public ErroCheckout(String codigoErro) {
        super(codigoErro);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }
}
