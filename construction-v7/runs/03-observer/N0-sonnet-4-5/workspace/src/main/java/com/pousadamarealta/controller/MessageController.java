package com.pousadamarealta.controller;

import com.pousadamarealta.dto.MessageDTO;
import com.pousadamarealta.service.NotificationService;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final NotificationService notificationService;

    public MessageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void getMessages(Context ctx) {
        List<MessageDTO> messages = notificationService.getSentMessages().stream()
            .map(MessageDTO::from)
            .toList();
        ctx.json(messages);
    }
}
