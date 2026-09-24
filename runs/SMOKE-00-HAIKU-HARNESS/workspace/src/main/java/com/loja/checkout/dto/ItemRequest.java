package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class ItemRequest {
    @JsonProperty("nome")
    private String nome;

    @JsonProperty("precoUnitario")
    private BigDecimal precoUnitario;

    @JsonProperty("quantidade")
    private Integer quantidade;

    @JsonProperty("pesoKg")
    private BigDecimal pesoKg;

    public ItemRequest() {}

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }
}
