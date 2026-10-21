package com.pousada.controller;

import com.pousada.dto.MessageResponse;
import com.pousada.service.MessageService;
import io.javalin.Javalin;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/messages", this::getMessages);
    }

    private void getMessages(io.javalin.http.Context ctx) {
        var messages = messageService.getAllMessages();
        var responses = messages.stream().map(MessageResponse::fromMessage).toList();
        ctx.json(responses);
    }
}
