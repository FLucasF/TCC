package com.loja.dominio;

public interface Persistencia {

    /** Carrega tudo o que está guardado (criando o que faltar e o catálogo inicial). */
    Estado carregar();

    /** Grava as mudanças de forma atômica. */
    void gravar(Mudancas mudancas);
}
