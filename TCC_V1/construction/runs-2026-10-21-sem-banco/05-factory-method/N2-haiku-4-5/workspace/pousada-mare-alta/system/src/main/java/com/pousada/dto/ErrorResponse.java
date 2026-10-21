package com.pousada.dto;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("error")
    public String error;

    @SerializedName("message")
    public String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
