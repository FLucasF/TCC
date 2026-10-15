package com.loja.checkout.validacao;

public class CheckoutException extends RuntimeException {

    private final ErroCodigo codigo;

    public CheckoutException(ErroCodigo codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public ErroCodigo codigo() {
        return codigo;
    }
}
