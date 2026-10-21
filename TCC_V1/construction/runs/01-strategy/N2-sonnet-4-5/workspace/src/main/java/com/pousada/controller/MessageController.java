package com.pousada.controller;

import com.pousada.domain.Message;
import com.pousada.service.MessageService;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<Map<String, Object>> response = messages.stream()
            .map(this::toMessageDto)
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private Map<String, Object> toMessageDto(Message message) {
        return Map.of(
            "id", message.getId(),
            "channel", message.getChannel().toString(),
            "to", message.getTo(),
            "subject", message.getSubject(),
            "body", message.getBody()
        );
    }
}
