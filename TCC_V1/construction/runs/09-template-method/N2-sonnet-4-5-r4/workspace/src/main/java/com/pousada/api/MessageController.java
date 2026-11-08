package com.pousada.api;

import com.pousada.api.dto.MessageResponse;
import com.pousada.service.MessageService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void register(Javalin app) {
        app.get("/messages", this::getMessages);
    }

    private void getMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::new)
            .toList();
        ctx.status(HttpStatus.OK);
        ctx.json(messages);
    }
}
