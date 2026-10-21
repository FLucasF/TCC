package com.mareaalta.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
    @JsonProperty("error")
    public String error;

    @JsonProperty("message")
    public String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
