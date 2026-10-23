package com.pousadamarealta.controller;

import com.pousadamarealta.model.Message;
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

    public void getAll(Context ctx) {
        List<Message> messages = messageService.findAll();
        List<Map<String, Object>> response = messages.stream()
                .map(this::toMap)
                .toList();
        ctx.json(response);
    }

    private Map<String, Object> toMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("channel", message.getChannel().name());
        map.put("to", message.getTo());
        map.put("subject", message.getSubject());
        map.put("body", message.getBody());
        map.put("status", message.getStatus().name());
        return map;
    }
}
