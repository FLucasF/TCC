package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemRequest(
        @JsonProperty("nome") String nome,
        @JsonProperty("precoUnitario") Double precoUnitario,
        @JsonProperty("quantidade") Integer quantidade,
        @JsonProperty("pesoKg") Double pesoKg
) {}
