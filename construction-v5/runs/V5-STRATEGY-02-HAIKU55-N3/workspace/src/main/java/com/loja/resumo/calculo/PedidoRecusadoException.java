package com.loja.resumo.calculo;

public class PedidoRecusadoException extends RuntimeException {

    private final String codigo;

    public PedidoRecusadoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
