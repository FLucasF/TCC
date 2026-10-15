package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
    @JsonProperty("nome")
    private String nome;

    @JsonProperty("precoUnitario")
    private double precoUnitario;

    @JsonProperty("quantidade")
    private int quantidade;

    @JsonProperty("pesoKg")
    private double pesoKg;

    public Item() {
    }

    public Item(String nome, double precoUnitario, int quantidade, double pesoKg) {
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

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }
}
