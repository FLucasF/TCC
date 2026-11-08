package com.pousadamarealta.controller;

import com.pousadamarealta.dto.MessageResponse;
import com.pousadamarealta.model.Message;
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
        List<MessageResponse> messages = messageRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        ctx.json(messages);
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
