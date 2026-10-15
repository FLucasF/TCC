package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErroDto(@JsonProperty("erro") String erro) {}
