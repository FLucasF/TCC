package com.loja.pedidos.dominio;

public class PedidoNaoEncontradoException extends RuntimeException {

    public PedidoNaoEncontradoException(String id) {
        super("Pedido não encontrado: " + id);
    }
}
