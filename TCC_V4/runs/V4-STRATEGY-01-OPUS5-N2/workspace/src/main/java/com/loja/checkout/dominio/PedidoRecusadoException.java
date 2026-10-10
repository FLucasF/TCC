package com.loja.checkout.dominio;

/** Pedido que nao da' para calcular. Carrega o codigo do problema. */
public class PedidoRecusadoException extends RuntimeException {

    private final ErroCheckout erro;

    public PedidoRecusadoException(ErroCheckout erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroCheckout erro() {
        return erro;
    }
}
