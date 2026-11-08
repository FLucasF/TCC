package com.marealta.api.controller;

import com.marealta.api.dto.MessageResponse;
import com.marealta.infrastructure.messaging.MessageStore;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageStore messageStore;

    public MessageController(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    public void list(Context ctx) {
        List<MessageResponse> messages = messageStore.findAll().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
