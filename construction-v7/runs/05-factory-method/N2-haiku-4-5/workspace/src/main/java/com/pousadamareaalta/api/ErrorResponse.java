package com.pousadamareaalta.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
    @JsonProperty("error") String error,
    @JsonProperty("message") String message
) {
}
