package com.loja.checkout.api;

public class ErroCheckout extends RuntimeException {

    private final transient CodigoErro codigo;

    public ErroCheckout(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
