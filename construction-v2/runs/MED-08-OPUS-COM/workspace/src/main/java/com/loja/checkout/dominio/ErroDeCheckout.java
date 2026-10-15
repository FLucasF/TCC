package com.loja.checkout.dominio;

public class ErroDeCheckout extends RuntimeException {

    private final CodigoErro codigo;

    public ErroDeCheckout(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
