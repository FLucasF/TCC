package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemRequest(
    @JsonProperty("nome") String nome,
    @JsonProperty("precoUnitario") double precoUnitario,
    @JsonProperty("quantidade") int quantidade,
    @JsonProperty("pesoKg") double pesoKg
) {}
