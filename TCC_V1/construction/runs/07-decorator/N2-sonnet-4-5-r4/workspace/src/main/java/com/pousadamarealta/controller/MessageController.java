package com.pousadamarealta.controller;

import com.pousadamarealta.dto.MessageDTO;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<MessageDTO> messages = messageService.getAllMessages().stream()
            .map(MessageDTO::from)
            .toList();
        ctx.json(messages);
    }
}
