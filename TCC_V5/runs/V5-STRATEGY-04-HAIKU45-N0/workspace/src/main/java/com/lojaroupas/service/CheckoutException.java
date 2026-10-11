package com.lojaroupas.service;

public class CheckoutException extends Exception {

    private String codigo;

    public CheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

}
