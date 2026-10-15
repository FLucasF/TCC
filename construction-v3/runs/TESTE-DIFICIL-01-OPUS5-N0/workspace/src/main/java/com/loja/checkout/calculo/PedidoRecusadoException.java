package com.loja.checkout.calculo;

/** O pedido nao pode ser calculado; carrega o codigo do problema. */
public class PedidoRecusadoException extends RuntimeException {

    private final String codigo;

    public PedidoRecusadoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
