package com.pousadamarealta.handler;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHandler {
    private final MessageRepository messageRepository;

    public MessageHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getAllMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
            .map(message -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", message.getId());
                map.put("channel", message.getChannel().name());
                map.put("to", message.getTo());
                map.put("subject", message.getSubject());
                map.put("body", message.getBody());
                return map;
            })
            .toList();
        ctx.json(response);
    }
}
