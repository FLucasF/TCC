package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemRequest {

    @JsonProperty("nome")
    private String nome;

    @JsonProperty("precoUnitario")
    private Double precoUnitario;

    @JsonProperty("quantidade")
    private Integer quantidade;

    @JsonProperty("pesoKg")
    private Double pesoKg;

    public ItemRequest() {
    }

    public ItemRequest(String nome, Double precoUnitario, Integer quantidade, Double pesoKg) {
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
