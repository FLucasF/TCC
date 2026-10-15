package com.loja.pedidos.dominio;

/** Algo que acontece com o pedido junto com a mudanca de situacao. */
@FunctionalInterface
public interface Efeito {

    void aplicar(Pedido pedido);
}
