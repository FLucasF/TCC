package com.loja.checkout.service;

public class ValidacaoException extends Exception {

    private final String codigoErro;

    public ValidacaoException(String codigoErro) {
        super(codigoErro);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }

}
