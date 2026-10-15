package com.loja.checkout.calculo;

public class ErroPedido extends RuntimeException {

    private final CodigoErro codigo;

    public ErroPedido(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
