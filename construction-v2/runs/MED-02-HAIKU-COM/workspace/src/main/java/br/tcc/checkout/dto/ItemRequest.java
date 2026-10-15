package br.tcc.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemRequest {
    @JsonProperty("nome")
    public String nome;

    @JsonProperty("precoUnitario")
    public Double precoUnitario;

    @JsonProperty("quantidade")
    public Integer quantidade;

    @JsonProperty("pesoKg")
    public Double pesoKg;
}
