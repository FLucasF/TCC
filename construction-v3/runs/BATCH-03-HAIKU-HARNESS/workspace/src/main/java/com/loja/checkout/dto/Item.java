package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
    @JsonProperty("nome")
    private String nome;

    @JsonProperty("precoUnitario")
    private Double precoUnitario;

    @JsonProperty("quantidade")
    private Integer quantidade;

    @JsonProperty("pesoKg")
    private Double pesoKg;

    public Item() {
    }

    public Item(String nome, Double precoUnitario, Integer quantidade, Double pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }

    public String getNome() {
        return nome;
    }

    public Double getPrecoUnitario() {
        return precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public Double getPesoKg() {
        return pesoKg;
    }
}
