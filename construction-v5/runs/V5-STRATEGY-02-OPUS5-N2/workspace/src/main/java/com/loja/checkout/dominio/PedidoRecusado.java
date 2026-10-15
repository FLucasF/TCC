package com.loja.checkout.dominio;

/** Pedido que não pode ser calculado: carrega o código do problema encontrado. */
public class PedidoRecusado extends RuntimeException {

    private final CodigoErro codigo;

    public PedidoRecusado(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
