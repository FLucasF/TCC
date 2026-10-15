package com.loja.checkout.erro;

/** Pedido que nao da para calcular. Carrega o codigo do problema encontrado. */
public class PedidoRecusado extends RuntimeException {

    private final Codigo codigo;

    public PedidoRecusado(Codigo codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public Codigo codigo() {
        return codigo;
    }
}
