package com.loja.pedidos.api;

public class ErroDoPedido extends RuntimeException {

    private final transient Erro erro;

    public ErroDoPedido(Erro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public Erro erro() {
        return erro;
    }
}
