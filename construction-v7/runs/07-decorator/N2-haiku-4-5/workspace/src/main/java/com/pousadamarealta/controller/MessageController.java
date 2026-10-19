package com.pousadamarealta.controller;

import com.pousadamarealta.api.MessageResponse;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> responses = new ArrayList<>();
        for (Message message : messageService.getAllMessages()) {
            responses.add(new MessageResponse(
                message.getId(),
                message.getChannel().getCode(),
                message.getTo(),
                message.getSubject(),
                message.getBody(),
                message.getStatus().getCode()
            ));
        }
        ctx.json(responses);
    }
}
