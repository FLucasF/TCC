package com.loja.checkout.dominio;

public class ErroNegocio extends RuntimeException {

    private final CodigoErro codigo;

    public ErroNegocio(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
