package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemCarrinho {
    private String nome;
    @JsonProperty("precoUnitario")
    private Double precoUnitario;
    private Integer quantidade;
    @JsonProperty("pesoKg")
    private Double pesoKg;

    public ItemCarrinho() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(Double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }
}
