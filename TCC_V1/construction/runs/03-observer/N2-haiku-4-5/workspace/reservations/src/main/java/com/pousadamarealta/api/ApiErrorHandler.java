package com.pousadamarealta.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.Map;

public class ApiErrorHandler {
    public static void handleException(Exception e, Context ctx) {
        e.printStackTrace();
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.json(Map.of(
                "error", "INTERNAL_SERVER_ERROR",
                "message", e.getMessage() != null ? e.getMessage() : "Internal server error"
        ));
    }
}
