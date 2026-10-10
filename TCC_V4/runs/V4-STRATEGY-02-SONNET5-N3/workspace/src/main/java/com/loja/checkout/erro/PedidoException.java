package com.loja.checkout.erro;

public class PedidoException extends RuntimeException {

    private final CodigoErro codigo;

    public PedidoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
