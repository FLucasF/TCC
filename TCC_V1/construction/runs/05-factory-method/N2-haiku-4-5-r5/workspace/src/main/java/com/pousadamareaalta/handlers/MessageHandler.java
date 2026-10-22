package com.pousadamareaalta.handlers;

import com.pousadamareaalta.repositories.MessageRepository;
import io.javalin.http.Context;

public class MessageHandler {
    private final MessageRepository messageRepository = new MessageRepository();

    public void list(Context ctx) {
        try {
            var messages = messageRepository.findAll();
            var response = messages.stream().map(m -> new Object() {
                public final Long id = m.id;
                public final String channel = m.channel;
                public final String to = m.to;
                public final String subject = m.subject;
                public final String body = m.body;
            }).toList();

            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500).json(new Object() {
                public final String error = "INTERNAL_ERROR";
                public final String message = e.getMessage();
            });
        }
    }
}
