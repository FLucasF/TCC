package com.pousadamarealta.api.controller;

import com.pousadamarealta.api.dto.MessageResponse;
import com.pousadamarealta.domain.service.MessageService;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) throws SQLException {
        List<MessageResponse> messages = messageService.getAllMessages().stream()
            .map(MessageResponse::new)
            .collect(Collectors.toList());
        ctx.json(messages);
    }
}
