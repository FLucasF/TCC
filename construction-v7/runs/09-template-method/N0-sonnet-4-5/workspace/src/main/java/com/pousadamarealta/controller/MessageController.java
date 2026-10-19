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

    public void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<Map<String, Object>> response = messages.stream()
                .map(msg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", msg.getId());
                    map.put("channel", msg.getChannel());
                    map.put("to", msg.getTo());
                    map.put("subject", msg.getSubject());
                    map.put("body", msg.getBody());
                    return map;
                })
                .toList();
        ctx.json(response);
    }
}
