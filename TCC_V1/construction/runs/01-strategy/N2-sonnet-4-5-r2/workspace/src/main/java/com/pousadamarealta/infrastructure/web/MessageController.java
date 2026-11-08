package com.pousadamarealta.infrastructure.web;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.service.MessageService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<Map<String, Object>> response = messages.stream()
            .map(this::toMessageDto)
            .toList();
        ctx.json(response);
    }

    private Map<String, Object> toMessageDto(Message message) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", message.getId());
        dto.put("channel", message.getChannel().name());
        dto.put("to", message.getTo());
        dto.put("subject", message.getSubject());
        dto.put("body", message.getBody());
        return dto;
    }
}
