package com.pousadamaralta.api.controller;

import com.pousadamaralta.api.dto.MessageResponse;
import com.pousadamaralta.domain.service.MessageService;
import io.javalin.http.Context;
import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void list(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
