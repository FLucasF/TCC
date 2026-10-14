package com.loja.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ProdutoDTO {
    @JsonProperty("sku")
    public String sku;

    @JsonProperty("nome")
    public String nome;

    @JsonProperty("preco")
    public BigDecimal preco;

    @JsonProperty("pesoKg")
    public BigDecimal pesoKg;

    @JsonProperty("estoque")
    public int estoque;

    @JsonProperty("foraDeLinha")
    public boolean foraDeLinha;

    public ProdutoDTO() {}

    public ProdutoDTO(String sku, String nome, BigDecimal preco, BigDecimal pesoKg, int estoque, boolean foraDeLinha) {
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
