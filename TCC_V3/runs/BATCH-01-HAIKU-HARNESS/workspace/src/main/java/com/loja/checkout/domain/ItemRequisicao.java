package com.loja.checkout.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemRequisicao {
    @JsonProperty("nome")
    private String nome;
    @JsonProperty("precoUnitario")
    private double precoUnitario;
    @JsonProperty("quantidade")
    private long quantidade;
    @JsonProperty("pesoKg")
    private double pesoKg;

    public ItemRequisicao() {}

    public ItemRequisicao(String nome, double precoUnitario, long quantidade, double pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public long getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(long quantidade) {
        this.quantidade = quantidade;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }
}
