package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemCarrinho {
    @JsonProperty
    private String nome;
    @JsonProperty
    private Double precoUnitario;
    @JsonProperty
    private Integer quantidade;
    @JsonProperty
    private Double pesoKg;

    public ItemCarrinho() {}

    public ItemCarrinho(String nome, Double precoUnitario, Integer quantidade, Double pesoKg) {
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
