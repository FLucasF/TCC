package com.pousada.controller;

import com.pousada.service.NotificationService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController {
    private final NotificationService notificationService;

    public MessageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void getMessages(Context ctx) {
        List<NotificationService.Message> messages = notificationService.getSentMessages();
        List<Map<String, Object>> response = messages.stream()
                .map(this::messageToMap)
                .toList();
        ctx.json(response);
    }

    private Map<String, Object> messageToMap(NotificationService.Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("channel", message.getChannel());
        map.put("to", message.getTo());
        map.put("subject", message.getSubject());
        map.put("body", message.getBody());
        return map;
    }
}
