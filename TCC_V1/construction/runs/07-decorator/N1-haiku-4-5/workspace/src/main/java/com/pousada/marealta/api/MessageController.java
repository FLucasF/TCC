package com.pousada.marealta.api;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.infrastructure.persistence.MessageRepository;
import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                List<Message> messages = messageRepository.findAll();
                List<Map<String, Object>> response = messages.stream().map(msg -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", msg.getId());
                    m.put("channel", msg.getChannel().name());
                    m.put("to", msg.getTo());
                    m.put("subject", msg.getSubject());
                    m.put("body", msg.getBody());
                    m.put("status", msg.getStatus().name());
                    return m;
                }).toList();
                ctx.json(response);
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
            }
        });
    }
}
