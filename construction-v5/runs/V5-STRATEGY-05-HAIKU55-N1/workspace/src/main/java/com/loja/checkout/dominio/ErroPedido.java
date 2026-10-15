package com.loja.checkout.dominio;

public class ErroPedido extends RuntimeException {

    private final CodigoErro codigo;

    public ErroPedido(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
