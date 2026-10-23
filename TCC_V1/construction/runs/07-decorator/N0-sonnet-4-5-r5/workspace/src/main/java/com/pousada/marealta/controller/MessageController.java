package com.pousada.marealta.controller;

import com.pousada.marealta.dto.MessageResponse;
import com.pousada.marealta.service.MessageService;
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
