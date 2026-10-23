package com.pousada.api;

import com.pousada.api.dto.MessageDto;
import com.pousada.infrastructure.MessageRepository;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<MessageDto> messages = messageRepository.findAll().stream()
            .map(MessageDto::from)
            .toList();
        ctx.json(messages);
    }
}
