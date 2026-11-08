package com.pousadamarealta.web.controller;

import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.web.dto.MessageDTO;
import com.pousadamarealta.web.mapper.DomainMapper;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<MessageDTO> messages = messageService.getAllMessages().stream()
            .map(DomainMapper::toDTO)
            .toList();
        ctx.json(messages);
    }
}
