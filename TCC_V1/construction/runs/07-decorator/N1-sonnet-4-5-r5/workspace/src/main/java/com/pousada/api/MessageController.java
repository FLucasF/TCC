package com.pousada.api;

import com.pousada.domain.Message;
import com.pousada.service.MessageService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<Map<String, Object>> response = messages.stream()
            .map(message -> {
                Map<String, Object> map = new HashMap<>();
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
