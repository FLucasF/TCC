package com.pousadamarealta.api.controller;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.messaging.MessageService;
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
            .map(message -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", message.getId());
                map.put("channel", message.getChannel().name());
                map.put("to", message.getTo());
                map.put("subject", message.getSubject());
                map.put("body", message.getBody());
                map.put("status", message.getStatus().name());
                return map;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
