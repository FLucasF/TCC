package com.pousada.marealta.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    public String error;
    public String message;

    public ApiResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public ApiResponse() {}
}
