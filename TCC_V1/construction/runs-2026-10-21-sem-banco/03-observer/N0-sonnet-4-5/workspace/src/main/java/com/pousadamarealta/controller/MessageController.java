package com.pousadamarealta.controller;

import com.pousadamarealta.dto.MessageResponse;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAll(Context ctx) {
        List<MessageResponse> messages = messageService.getAll().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
