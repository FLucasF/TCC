package com.pousadamarealta.api.handler;

import com.pousadamarealta.api.dto.MessageResponse;
import com.pousadamarealta.api.mapper.ApiMapper;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageHandler {
    private final MessageService messageService;

    public MessageHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<MessageResponse> response = messages.stream()
            .map(ApiMapper::toMessageResponse)
            .toList();
        ctx.json(response);
    }
}
