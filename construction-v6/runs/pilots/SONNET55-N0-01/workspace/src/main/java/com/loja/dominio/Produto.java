package com.loja.dominio;

import java.math.BigDecimal;

public class Produto {

    private final String sku;
    private final String nome;
    private final BigDecimal preco;
    private final BigDecimal pesoKg;
    private int estoque;
    private final boolean foraDeLinha;

    public Produto(String sku, String nome, BigDecimal preco, BigDecimal pesoKg, int estoque, boolean foraDeLinha) {
        this.sku = sku;
        this.nome = nome;
        this.preco = preco;
        this.pesoKg = pesoKg;
        this.estoque = estoque;
        this.foraDeLinha = foraDeLinha;
    }

    public String sku() { return sku; }
    public String nome() { return nome; }
    public BigDecimal preco() { return preco; }
    public BigDecimal pesoKg() { return pesoKg; }
    public int estoque() { return estoque; }
    public boolean foraDeLinha() { return foraDeLinha; }

    public void baixar(int quantidade) {
        if (quantidade > estoque) {
            throw new IllegalStateException("estoque negativo: " + sku);
        }
        estoque -= quantidade;
    }

    public void devolver(int quantidade) {
        estoque += quantidade;
    }
}
