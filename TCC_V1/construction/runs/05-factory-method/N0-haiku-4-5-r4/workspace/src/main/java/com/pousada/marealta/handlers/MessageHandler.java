package com.pousada.marealta.handlers;

import com.pousada.marealta.repository.MessageRepository;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                var messages = MessageRepository.findAll();
                ctx.json(messages);
            } catch (Exception e) {
                logger.error("Error fetching messages", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }

    record ErrorResponse(String error, String message) {}
}
