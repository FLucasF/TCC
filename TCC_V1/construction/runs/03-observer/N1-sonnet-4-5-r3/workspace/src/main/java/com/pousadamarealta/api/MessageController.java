package com.pousadamarealta.api;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
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
            .map(msg -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", msg.getId());
                map.put("channel", msg.getChannel());
                map.put("to", msg.getTo());
                map.put("subject", msg.getSubject());
                map.put("body", msg.getBody());
                return map;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
