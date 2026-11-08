package com.marealta.pousada.controller;

import com.marealta.pousada.dto.MessageDto;
import com.marealta.pousada.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<MessageDto> messages = messageService.getAllMessages().stream()
            .map(MessageDto::from)
            .toList();
        ctx.json(messages);
    }
}
