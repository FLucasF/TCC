package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErroResponse(
    @JsonProperty("erro") String erro
) {}
