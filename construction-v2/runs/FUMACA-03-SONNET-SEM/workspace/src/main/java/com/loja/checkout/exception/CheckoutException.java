package com.loja.checkout.exception;

public class CheckoutException extends RuntimeException {

    private final ErroCodigo codigo;

    public CheckoutException(ErroCodigo codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public ErroCodigo getCodigo() {
        return codigo;
    }
}
