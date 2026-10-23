package br.com.pousada.marealta.controller;

import br.com.pousada.marealta.model.Message;
import br.com.pousada.marealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
            .map(this::toMessageDto)
            .toList();
        ctx.json(response);
    }

    private Map<String, Object> toMessageDto(Message message) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", message.getId());
        dto.put("channel", message.getChannel());
        dto.put("to", message.getTo());
        dto.put("subject", message.getSubject());
        dto.put("body", message.getBody());
        return dto;
    }
}
