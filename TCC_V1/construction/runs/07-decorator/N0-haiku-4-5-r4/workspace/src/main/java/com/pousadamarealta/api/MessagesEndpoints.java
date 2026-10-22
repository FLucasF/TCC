package com.pousadamarealta.api;

import io.javalin.Javalin;
import com.pousadamarealta.repository.MessageRepository;
import java.sql.SQLException;

public class MessagesEndpoints {
    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                ctx.json(MessageRepository.findAll());
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
