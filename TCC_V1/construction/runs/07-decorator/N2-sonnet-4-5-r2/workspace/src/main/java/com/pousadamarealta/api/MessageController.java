package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.MessageResponse;
import com.pousadamarealta.messaging.MessagingService;
import io.javalin.http.Context;

import java.util.List;

public final class MessageController {
    private final MessagingService messagingService;

    public MessageController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> messages = messagingService.getAllMessages().stream()
            .map(MessageResponse::from)
            .toList();
        ctx.json(messages);
    }
}
