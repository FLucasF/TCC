package br.pousada.marealta.api;

import java.util.HashMap;
import java.util.Map;

public class ErrorResponse {
    public static Map<String, Object> error(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", code);
        response.put("message", message);
        return response;
    }
}
