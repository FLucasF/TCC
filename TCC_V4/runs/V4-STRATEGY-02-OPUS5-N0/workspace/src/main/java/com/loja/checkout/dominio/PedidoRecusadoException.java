package com.loja.checkout.dominio;

/** Pedido recusado: carrega apenas o código do problema encontrado. */
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
