package com.pousadamarealta.controller;

import com.pousadamarealta.dto.MessageDto;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<MessageDto> messages = messageRepository.findAll().stream()
            .map(MessageDto::from)
            .collect(Collectors.toList());

        ctx.json(messages);
    }
}
