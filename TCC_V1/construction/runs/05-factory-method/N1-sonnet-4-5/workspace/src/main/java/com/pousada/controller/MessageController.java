package com.pousada.controller;

import com.pousada.dto.MessageResponse;
import com.pousada.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
