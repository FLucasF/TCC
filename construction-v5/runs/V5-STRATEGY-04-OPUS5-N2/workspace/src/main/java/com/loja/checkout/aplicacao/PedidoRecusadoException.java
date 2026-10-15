package com.loja.checkout.aplicacao;

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
