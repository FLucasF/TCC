package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.MessageResponse;
import com.pousadamarealta.domain.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAll().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
