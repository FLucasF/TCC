package com.loja.pedidos.dominio;

@FunctionalInterface
public interface Transicao {
    Efeito aplicar(Pedido pedido);
}
