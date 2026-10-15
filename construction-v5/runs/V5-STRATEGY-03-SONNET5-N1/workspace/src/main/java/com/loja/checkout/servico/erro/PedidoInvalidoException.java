package com.loja.checkout.servico.erro;

public class PedidoInvalidoException extends RuntimeException {

    private final CodigoErro codigo;

    public PedidoInvalidoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
