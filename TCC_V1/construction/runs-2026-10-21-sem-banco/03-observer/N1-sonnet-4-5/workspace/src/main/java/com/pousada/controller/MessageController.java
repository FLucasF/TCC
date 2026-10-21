package com.pousada.controller;

import com.pousada.dto.MessageResponse;
import com.pousada.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> messages = messageRepository.findAll().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
