package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.MessageDTO;
import com.pousadamarealta.api.mapper.MessageMapper;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getAllMessages(Context ctx) {
        List<MessageDTO> messages = messageRepository.findAll().stream()
            .map(MessageMapper::toDTO)
            .collect(Collectors.toList());
        ctx.json(messages);
    }
}
