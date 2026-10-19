package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.MessageDTO;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<MessageDTO> messages = messageService.getAllMessages().stream()
            .map(this::toDTO)
            .toList();
        ctx.json(messages);
    }

    private MessageDTO toDTO(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
