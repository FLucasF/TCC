package com.pousada.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class ErrorResponse {
    @JsonProperty("error")
    public String error;

    @JsonProperty("message")
    public String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public static String error(String code, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", code);
        response.put("message", message);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\":\"" + code + "\",\"message\":\"" + message + "\"}";
        }
    }
}
