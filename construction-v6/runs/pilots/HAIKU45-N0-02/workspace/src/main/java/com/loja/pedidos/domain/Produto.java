package com.loja.pedidos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produto")
public class Produto {
    @Id
    private String sku;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(nullable = false)
    private BigDecimal pesoKg;

    @Column(nullable = false)
    private int estoque;

    @Column(nullable = false)
    private boolean foraDeLinha;

    public Produto() {}

    public Produto(String sku, String nome, BigDecimal preco, BigDecimal pesoKg, int estoque, boolean foraDeLinha) {
        this.sku = sku;
        this.nome = nome;
        this.preco = preco;
        this.pesoKg = pesoKg;
        this.estoque = estoque;
        this.foraDeLinha = foraDeLinha;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public int getEstoque() {
        return estoque;
    }

    public void setEstoque(int estoque) {
        this.estoque = estoque;
    }

    public boolean isForaDeLinha() {
        return foraDeLinha;
    }

    public void setForaDeLinha(boolean foraDeLinha) {
        this.foraDeLinha = foraDeLinha;
    }
}
