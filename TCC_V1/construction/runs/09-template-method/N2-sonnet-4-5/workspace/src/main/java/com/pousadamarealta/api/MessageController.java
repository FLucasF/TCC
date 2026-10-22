package com.pousadamarealta.api;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void listMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private Map<String, Object> toDto(Message message) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", message.getId());
        dto.put("channel", message.getChannel());
        dto.put("to", message.getTo());
        dto.put("subject", message.getSubject());
        dto.put("body", message.getBody());
        return dto;
    }
}
