package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
    @JsonProperty("nome")
    public String nome;

    @JsonProperty("precoUnitario")
    public double precoUnitario;

    @JsonProperty("quantidade")
    public int quantidade;

    @JsonProperty("pesoKg")
    public double pesoKg;

    public Item() {}

    public Item(String nome, double precoUnitario, int quantidade, double pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }
}
