package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private String error;
    private String message;

    public ApiResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public static ApiResponse error(String code, String message) {
        return new ApiResponse(code, message);
    }
}
