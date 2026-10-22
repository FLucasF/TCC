package com.pousadamarealta.api;

import com.pousadamarealta.api.dto.MessageDTO;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;
import java.util.List;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<MessageDTO> dtos = messages.stream()
            .map(MessageDTO::from)
            .toList();
        ctx.json(dtos);
    }
}
