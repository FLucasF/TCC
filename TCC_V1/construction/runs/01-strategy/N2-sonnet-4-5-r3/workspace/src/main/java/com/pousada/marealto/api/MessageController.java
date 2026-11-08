package com.pousada.marealto.api;

import com.pousada.marealto.messaging.Message;
import com.pousada.marealto.service.MessageService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<Map<String, Object>> response = messages.stream()
            .map(this::messageToMap)
            .toList();
        ctx.json(response);
    }

    private Map<String, Object> messageToMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("channel", message.getChannel().name());
        map.put("to", message.getTo());
        map.put("subject", message.getSubject());
        map.put("body", message.getBody());
        return map;
    }
}
