package com.lojaroupas.dto;

import com.google.gson.annotations.SerializedName;

public class ItemCarrinho {
    @SerializedName("nome")
    public String nome;

    @SerializedName("precoUnitario")
    public Double precoUnitario;

    @SerializedName("quantidade")
    public Integer quantidade;

    @SerializedName("pesoKg")
    public Double pesoKg;

    public ItemCarrinho() {}

    public ItemCarrinho(String nome, Double precoUnitario, Integer quantidade, Double pesoKg) {
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
    }

    public Double getValorTotal() {
        return precoUnitario * quantidade;
    }

    public Double getPesoTotal() {
        return pesoKg * quantidade;
    }
}
