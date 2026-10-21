package com.pousadamarealta.api;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<Map<String, Object>> response = messages.stream()
            .map(this::toDto)
            .toList();
        ctx.json(response);
    }

    private Map<String, Object> toDto(Message message) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", message.id());
        dto.put("channel", message.channel());
        dto.put("to", message.to());
        dto.put("subject", message.subject());
        dto.put("body", message.body());
        dto.put("status", message.status().name());
        return dto;
    }
}
