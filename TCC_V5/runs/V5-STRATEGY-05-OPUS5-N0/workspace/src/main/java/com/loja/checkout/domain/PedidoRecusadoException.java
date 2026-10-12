package com.loja.checkout.domain;

/** Lancada quando o pedido nao pode ser calculado. */
public class PedidoRecusadoException extends RuntimeException {

    private final ErroPedido erro;

    public PedidoRecusadoException(ErroPedido erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroPedido erro() {
        return erro;
    }
}
