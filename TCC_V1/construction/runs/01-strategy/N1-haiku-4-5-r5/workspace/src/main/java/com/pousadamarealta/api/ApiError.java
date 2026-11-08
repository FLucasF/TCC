package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiError {
    @JsonProperty("error")
    public final String code;

    @JsonProperty("message")
    public final String message;

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
