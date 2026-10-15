package com.loja.checkout.api;

/** Pedido que não dá para calcular: carrega só o código do problema. */
public class PedidoRecusado extends RuntimeException {

    private final String codigo;

    public PedidoRecusado(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
