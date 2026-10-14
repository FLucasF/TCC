package com.loja.dominio;

/** Sem banco: guarda só em memória (usado quando DB_URL não está definido, como nos testes). */
public class PersistenciaMemoria implements Persistencia {

    private final Estado estado = new Estado();

    public PersistenciaMemoria() {
        Catalogo.inicial().forEach(p -> estado.produtos.put(p.sku(), p));
    }

    @Override
    public Estado carregar() {
        return estado;
    }

    @Override
    public void gravar(Mudancas mudancas) {
    }
}
