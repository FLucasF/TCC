package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorDto {
    @JsonProperty
    private String error;

    public ErrorDto(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
