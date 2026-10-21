package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorDto {
    @JsonProperty("error")
    public String error;

    @JsonProperty("message")
    public String message;

    public ErrorDto(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
