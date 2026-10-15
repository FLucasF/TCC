package br.tcc.checkout.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemDto(
    String nome,
    @JsonProperty("precoUnitario")
    double precoUnitario,
    int quantidade,
    @JsonProperty("pesoKg")
    double pesoKg
) {}
