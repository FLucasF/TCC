package com.pousadamarealta.controller;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController() {
        this.messageRepository = new MessageRepository();
    }

    public void listMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
            .map(this::toJson)
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private Map<String, Object> toJson(Message m) {
        Map<String, Object> json = new HashMap<>();
        json.put("id", m.getId());
        json.put("channel", m.getChannel());
        json.put("to", m.getTo());
        json.put("subject", m.getSubject());
        json.put("body", m.getBody());
        return json;
    }
}
