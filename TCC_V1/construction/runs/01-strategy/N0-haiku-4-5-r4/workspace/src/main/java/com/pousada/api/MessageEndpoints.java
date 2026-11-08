package com.pousada.api;

import com.pousada.service.MessageService;
import io.javalin.Javalin;
import java.sql.SQLException;

public class MessageEndpoints {
    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                var messages = new MessageService().getMessages();
                ctx.json(messages);
            } catch (SQLException e) {
                ctx.status(500).json(new Object() {
                    public final String error = "DATABASE_ERROR";
                    public final String message = e.getMessage();
                });
            }
        });
    }
}
