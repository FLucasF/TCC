package com.loja.checkout.dominio;

public class RecusaPedido extends RuntimeException {

    private final Erro erro;

    public RecusaPedido(Erro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public Erro erro() {
        return erro;
    }
}
