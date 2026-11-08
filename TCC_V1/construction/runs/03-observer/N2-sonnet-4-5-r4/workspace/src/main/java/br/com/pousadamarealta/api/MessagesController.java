package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.MessageDTO;
import br.com.pousadamarealta.repository.MessageRepository;
import io.javalin.Javalin;

import java.util.List;

public class MessagesController {
    private final MessageRepository messageRepository;

    public MessagesController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void register(Javalin app) {
        app.get("/messages", ctx -> {
            List<MessageDTO> messages = messageRepository.findAll().stream()
                .map(MessageDTO::from)
                .toList();
            ctx.json(messages);
        });
    }
}
