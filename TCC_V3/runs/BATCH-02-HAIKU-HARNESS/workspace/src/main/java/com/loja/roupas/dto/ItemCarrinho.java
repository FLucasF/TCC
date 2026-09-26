package com.loja.roupas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemCarrinho {
    @JsonProperty("nome")
    private String nome;

    @JsonProperty("precoUnitario")
    private Double precoUnitario;

    @JsonProperty("quantidade")
    private Integer quantidade;

    @JsonProperty("pesoKg")
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

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setPrecoUnitario(Double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }
}
