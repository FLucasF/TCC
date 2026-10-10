package com.loja.checkout.dominio;

/** Pedido que nao pode ser calculado. Carrega o codigo do problema encontrado. */
public class PedidoRecusado extends RuntimeException {

    private final Erro erro;

    public PedidoRecusado(Erro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public Erro erro() {
        return erro;
    }
}
