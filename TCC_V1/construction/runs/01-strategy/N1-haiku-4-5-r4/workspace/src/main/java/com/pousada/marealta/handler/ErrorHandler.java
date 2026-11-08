package com.pousada.marealta.handler;

import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

public class ErrorHandler {
    public static void handleException(Context ctx, Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Internal server error";

        Map<Integer, String> errorMap = new HashMap<>();
        errorMap.put(400, "INVALID_REQUEST");
        errorMap.put(404, "NOT_FOUND");
        errorMap.put(409, "CONFLICT");
        errorMap.put(422, "UNPROCESSABLE_ENTITY");

        int status = 400;
        String errorCode = "INVALID_REQUEST";

        if (message.equals("ROOM_NOT_FOUND")) {
            status = 404;
            errorCode = "ROOM_NOT_FOUND";
        } else if (message.equals("RESERVATION_NOT_FOUND")) {
            status = 404;
            errorCode = "RESERVATION_NOT_FOUND";
        } else if (message.equals("ROOM_UNAVAILABLE")) {
            status = 409;
            errorCode = "ROOM_UNAVAILABLE";
        } else if (message.equals("OPERATION_NOT_ALLOWED")) {
            status = 409;
            errorCode = "OPERATION_NOT_ALLOWED";
        } else if (message.equals("CAPACITY_EXCEEDED")) {
            status = 422;
            errorCode = "CAPACITY_EXCEEDED";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);

        ctx.status(status).json(response);
    }

    public static void handle400(Context ctx, String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);
        ctx.status(400).json(response);
    }

    public static void handle404(Context ctx, String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);
        ctx.status(404).json(response);
    }

    public static void handle409(Context ctx, String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);
        ctx.status(409).json(response);
    }

    public static void handle422(Context ctx, String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);
        ctx.status(422).json(response);
    }
}
