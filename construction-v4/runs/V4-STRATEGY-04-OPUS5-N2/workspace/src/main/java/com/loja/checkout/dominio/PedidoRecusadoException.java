package com.loja.checkout.dominio;

/** Pedido recusado: o servico devolve so o codigo do problema. */
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
