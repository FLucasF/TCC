package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemCarrinho {
    @JsonProperty("nome")
    public String nome;

    @JsonProperty("precoUnitario")
    public Double precoUnitario;

    @JsonProperty("quantidade")
    public Integer quantidade;

    @JsonProperty("pesoKg")
    public Double pesoKg;

    public ItemCarrinho() {
    }

    public ItemCarrinho(String nome, Double precoUnitario, Integer quantidade, Double pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }
}
