package br.com.marealta.api.controller;

import br.com.marealta.api.dto.MessageResponse;
import br.com.marealta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void listMessages(Context ctx) {
        List<MessageResponse> messages = messageService.findAllMessages().stream()
            .map(MessageResponse::from)
            .toList();

        ctx.json(messages);
    }
}
