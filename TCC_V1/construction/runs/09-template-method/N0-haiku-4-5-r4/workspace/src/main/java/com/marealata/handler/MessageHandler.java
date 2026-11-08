package com.marealata.handler;

import com.marealata.dao.MessageDao;
import com.marealata.model.Message;
import io.javalin.Javalin;
import java.sql.SQLException;
import java.util.List;

public class MessageHandler {
    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                List<Message> messages = MessageDao.findAll();
                ctx.json(messages);
            } catch (SQLException e) {
                ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Database error"));
            }
        });
    }
}
