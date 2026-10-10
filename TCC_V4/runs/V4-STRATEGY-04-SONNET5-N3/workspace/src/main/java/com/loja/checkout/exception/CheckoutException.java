package com.loja.checkout.exception;

public class CheckoutException extends RuntimeException {

    public CheckoutException(String codigo) {
        super(codigo);
    }

    public String getCodigo() {
        return getMessage();
    }
}
