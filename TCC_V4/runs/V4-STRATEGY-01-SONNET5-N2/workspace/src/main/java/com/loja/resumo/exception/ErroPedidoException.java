package com.loja.resumo.exception;

public class ErroPedidoException extends RuntimeException {

    private final String codigo;

    public ErroPedidoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
