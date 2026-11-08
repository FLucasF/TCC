package com.pousadamarealta.controller;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.repository.MessageRepository;
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
                .map(this::messageToMap)
                .toList();
        ctx.json(response);
    }

    private Map<String, Object> messageToMap(Message m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("channel", m.getChannel());
        map.put("to", m.getTo());
        map.put("subject", m.getSubject());
        map.put("body", m.getBody());
        return map;
    }
}
