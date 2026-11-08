package com.pousadamarealta.routes;

import io.javalin.Javalin;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.ErrorResponse;

public class MessageRoutes {
    public static void register(Javalin app) throws Exception {
        MessageRepository messageRepository = new MessageRepository();

        app.get("/messages", ctx -> {
            try {
                ctx.json(messageRepository.findAll());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }
}
