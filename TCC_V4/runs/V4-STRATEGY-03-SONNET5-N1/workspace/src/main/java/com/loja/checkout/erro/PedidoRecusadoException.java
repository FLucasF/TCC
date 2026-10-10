package com.loja.checkout.erro;

public class PedidoRecusadoException extends RuntimeException {

    private final CodigoErro codigo;

    public PedidoRecusadoException(CodigoErro codigo) {
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
