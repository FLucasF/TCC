package com.pousadamarealta.api;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {

    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();

        List<Map<String, Object>> response = messages.stream()
            .map(message -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", message.getId());
                map.put("channel", message.getChannel().name());
                map.put("to", message.getTo());
                map.put("subject", message.getSubject());
                map.put("body", message.getBody());
                return map;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
