package com.pousada.api;

import com.pousada.domain.Message;
import com.pousada.repository.MessageRepository;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessagesApiHandler {
    public static void register(Javalin app, MessageRepository messageRepo) {
        app.get("/messages", ctx -> {
            List<Message> messages = messageRepo.findAll();
            List<Map<String, Object>> response = new ArrayList<>();

            for (Message msg : messages) {
                response.add(Map.of(
                    "id", msg.getId(),
                    "channel", msg.getChannel(),
                    "to", msg.getTo(),
                    "subject", msg.getSubject(),
                    "body", msg.getBody()
                ));
            }

            ctx.json(response);
        });
    }
}
