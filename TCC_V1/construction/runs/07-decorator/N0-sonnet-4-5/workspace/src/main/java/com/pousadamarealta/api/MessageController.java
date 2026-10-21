package com.pousadamarealta.api;

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
        List<Map<String, Object>> messages = messageService.getAllMessages().stream()
                .map(message -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", message.getId());
                    map.put("channel", message.getChannel());
                    map.put("to", message.getTo());
                    map.put("subject", message.getSubject());
                    map.put("body", message.getBody());
                    map.put("status", message.getStatus());
                    return map;
                })
                .toList();

        ctx.json(messages);
    }
}
