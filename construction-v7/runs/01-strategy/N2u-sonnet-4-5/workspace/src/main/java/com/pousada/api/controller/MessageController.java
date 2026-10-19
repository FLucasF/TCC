package com.pousada.api.controller;

import com.pousada.api.dto.MessageResponse;
import com.pousada.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
