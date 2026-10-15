package com.loja.checkout.exception;

import com.loja.checkout.enums.CodigoErro;

public class CheckoutException extends RuntimeException {
    private final CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
