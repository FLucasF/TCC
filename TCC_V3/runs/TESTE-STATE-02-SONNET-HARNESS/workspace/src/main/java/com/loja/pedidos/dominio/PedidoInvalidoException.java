package com.loja.pedidos.dominio;

public class PedidoInvalidoException extends RuntimeException {

    public PedidoInvalidoException() {
        super("Pedido inválido");
    }
}
