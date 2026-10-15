package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ItemRequest(
    @JsonProperty("nome") String nome,
    @JsonProperty("precoUnitario") BigDecimal precoUnitario,
    @JsonProperty("quantidade") Integer quantidade,
    @JsonProperty("pesoKg") BigDecimal pesoKg
) {}
