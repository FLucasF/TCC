package com.pousadamareal.controller;

import com.pousadamareal.api.MessageResponse;
import com.pousadamareal.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void listMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::new)
            .toList();
        ctx.json(messages);
    }
}
