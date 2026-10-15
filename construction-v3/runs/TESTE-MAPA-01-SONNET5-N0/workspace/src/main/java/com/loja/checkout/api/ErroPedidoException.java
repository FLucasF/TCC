package com.loja.checkout.api;

public class ErroPedidoException extends RuntimeException {

    private final CodigoErro codigo;

    public ErroPedidoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
