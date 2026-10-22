package br.com.marealte.controller;

import br.com.marealte.repository.MessageRepository;
import io.javalin.http.Context;

public class MessageController {

    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        ctx.json(messageRepository.findAll());
    }
}
