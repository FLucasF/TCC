package br.com.pousada.marealta.api;

import br.com.pousada.marealta.api.dto.MessageResponse;
import br.com.pousada.marealta.service.MessageRepository;
import io.javalin.Javalin;

public class MessageController {
    public static void register(Javalin app, MessageRepository messageRepository) {
        app.get("/messages", ctx -> {
            var messages = messageRepository.getAll()
                    .stream()
                    .map(MessageResponse::from)
                    .toList();
            ctx.json(messages);
        });
    }
}
