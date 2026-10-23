package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
