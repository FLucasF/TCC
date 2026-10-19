package com.pousadamarealta.handler;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHandler {
    private final MessageService messageService;

    public MessageHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<Map<String, Object>> messages = messageService.getAllMessages().stream()
            .map(this::toDto)
            .toList();
        ctx.json(messages);
    }

    private Map<String, Object> toDto(Message message) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", message.getId());
        dto.put("channel", message.getChannel());
        dto.put("to", message.getTo());
        dto.put("subject", message.getSubject());
        dto.put("body", message.getBody());
        return dto;
    }
}
