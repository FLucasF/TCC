package com.pousadamarealta.controller;

import com.pousadamarealta.dto.response.MessageResponse;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::new)
            .toList();
        ctx.json(messages);
    }
}
