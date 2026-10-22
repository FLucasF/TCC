package com.pousada.api;

import com.pousada.service.MessageService;
import io.javalin.Javalin;
import java.sql.SQLException;

public class MessageAPI {
    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                var messages = MessageService.getAllMessages();
                ctx.json(messages);
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
