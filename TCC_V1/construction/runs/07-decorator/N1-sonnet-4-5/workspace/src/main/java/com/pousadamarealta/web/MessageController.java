package com.pousadamarealta.web;

import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.web.dto.MessageResponse;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void listMessages(Context ctx) {
        List<MessageResponse> messages = messageService.findAll()
                .stream()
                .map(MessageResponse::from)
                .toList();
        ctx.json(messages);
    }
}
