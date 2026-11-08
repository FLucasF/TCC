package com.pousadamarealta.api;

import com.pousadamarealta.domain.Message;
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
        List<MessageDto> messages = messageService.getAllMessages().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        ctx.json(messages);
    }

    private MessageDto toDto(Message message) {
        return new MessageDto(
            message.getId(),
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }

    record MessageDto(
        long id,
        String channel,
        String to,
        String subject,
        String body
    ) {}
}
