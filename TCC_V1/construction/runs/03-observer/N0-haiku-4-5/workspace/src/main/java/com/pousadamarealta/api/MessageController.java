package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.MessageResponse;
import com.pousadamarealta.notification.MessageStore;
import io.javalin.http.Context;

public class MessageController {
    private final MessageStore messageStore;

    public MessageController(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    public void getMessages(Context ctx) {
        var messages = messageStore.getAllMessages();
        var response = messages.stream().map(MessageResponse::from).toList();
        ctx.json(response);
    }
}
