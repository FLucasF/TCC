package br.com.pousada.controller;

import br.com.pousada.repository.MessageRepository;
import io.javalin.Javalin;

public class MessageController {
    private static final MessageRepository messageRepository = new MessageRepository();

    public static void routes(Javalin app) {
        app.get("/messages", ctx -> {
            var messages = messageRepository.findAll();
            ctx.json(messages);
        });
    }
}
