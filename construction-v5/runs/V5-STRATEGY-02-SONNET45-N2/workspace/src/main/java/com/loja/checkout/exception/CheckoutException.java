package com.loja.checkout.exception;

public class CheckoutException extends RuntimeException {
    private final CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
