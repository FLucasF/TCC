package br.com.maralta.controller;

import br.com.maralta.controller.dto.MessageResponse;
import br.com.maralta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void listMessages(Context ctx) {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::from)
            .toList();

        ctx.json(messages);
    }
}
