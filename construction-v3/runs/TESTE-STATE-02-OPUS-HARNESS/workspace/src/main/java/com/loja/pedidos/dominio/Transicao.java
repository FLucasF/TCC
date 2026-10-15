package com.loja.pedidos.dominio;

public record Transicao(Situacao destino, Efeito efeito) {

    public Transicao(Situacao destino) {
        this(destino, Efeito.NENHUM);
    }
}
