package com.pousada.controller;

import com.pousada.dto.MessageResponse;
import com.pousada.service.MessageService;
import io.javalin.Javalin;

import java.util.List;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/messages", ctx -> {
            List<MessageResponse> messages = messageService.getAllMessages().stream()
                .map(MessageResponse::new)
                .collect(Collectors.toList());
            ctx.json(messages);
        });
    }
}
