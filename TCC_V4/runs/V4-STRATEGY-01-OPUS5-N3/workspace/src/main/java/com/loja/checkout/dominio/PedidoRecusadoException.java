package com.loja.checkout.dominio;

public class PedidoRecusadoException extends RuntimeException {

    private final Erro erro;

    public PedidoRecusadoException(Erro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public Erro erro() {
        return erro;
    }
}
