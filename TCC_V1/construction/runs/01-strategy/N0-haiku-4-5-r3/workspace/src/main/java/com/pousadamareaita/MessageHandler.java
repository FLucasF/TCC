package com.pousadamareaita;

import com.pousadamareaita.model.Message;
import com.pousadamareaita.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class MessageHandler {
    private static final MessageRepository messageRepo = new MessageRepository();

    public static void listMessages(Context ctx) {
        List<Message> messages = messageRepo.findAll();

        List<java.util.Map<String, Object>> response = messages.stream()
            .map(message -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", message.getId());
                m.put("channel", message.getChannel());
                m.put("to", message.getTo());
                m.put("subject", message.getSubject() != null ? message.getSubject() : "");
                m.put("body", message.getBody());
                return m;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
