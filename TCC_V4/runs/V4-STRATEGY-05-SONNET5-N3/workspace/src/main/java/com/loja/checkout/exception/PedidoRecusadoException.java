package com.loja.checkout.exception;

public class PedidoRecusadoException extends RuntimeException {

    private final CodigoErro codigo;

    public PedidoRecusadoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
