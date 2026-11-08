package com.pousada.marealta.controller;

import com.pousada.marealta.repository.MessageRepository;
import io.javalin.Javalin;

import java.sql.SQLException;

public class MessageController {
    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                var messages = new MessageRepository().findAll();
                ctx.json(messages.stream().map(m -> new Object() {
                    public final long id = m.getId();
                    public final String channel = m.getChannel().name();
                    public final String to = m.getTo();
                    public final String subject = m.getSubject();
                    public final String body = m.getBody();
                }).toList());
            } catch (SQLException e) {
                ctx.status(500);
                ctx.json(new Object() {
                    public final String error = "DATABASE_ERROR";
                    public final String message = e.getMessage();
                });
            }
        });
    }
}
