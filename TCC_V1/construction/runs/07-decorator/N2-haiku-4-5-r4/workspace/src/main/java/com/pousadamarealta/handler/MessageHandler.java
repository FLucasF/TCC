package com.pousadamarealta.handler;

import com.pousadamarealta.service.MessageService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHandler {
    private final MessageService messageService;

    public MessageHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    public void listMessages(Context ctx) {
        List<Map<String, Object>> messages = messageService.getAllMessages().stream()
                .map(msg -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("id", msg.id);
                    msgMap.put("channel", msg.channel);
                    msgMap.put("to", msg.to);
                    msgMap.put("subject", msg.subject);
                    msgMap.put("body", msg.body);
                    msgMap.put("status", msg.status.name());
                    return msgMap;
                })
                .toList();

        ctx.json(messages);
    }
}
