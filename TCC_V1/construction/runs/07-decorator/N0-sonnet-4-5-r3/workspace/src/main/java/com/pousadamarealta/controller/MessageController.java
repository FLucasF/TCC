package com.pousadamarealta.controller;

import com.pousadamarealta.dto.MessageDTO;
import com.pousadamarealta.mapper.ModelMapper;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<MessageDTO> messages = messageService.getAllMessages().stream()
            .map(ModelMapper::toDTO)
            .collect(Collectors.toList());
        ctx.json(messages);
    }
}
