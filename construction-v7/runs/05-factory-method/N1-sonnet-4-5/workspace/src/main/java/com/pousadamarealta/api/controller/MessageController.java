package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.MessageResponse;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void listMessages(Context ctx) {
        List<MessageResponse> messages = messageRepository.findAll().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
