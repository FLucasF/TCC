package com.pousada.api;

import io.javalin.Javalin;
import com.pousada.db.Database;
import com.pousada.model.Message;
import com.pousada.repository.MessageRepository;
import java.util.List;
import java.util.Map;

public class MessageController {
    public static void setup(Javalin app, Database db) {
        MessageRepository messageRepo = new MessageRepository(db);

        app.get("/messages", ctx -> {
            List<Message> messages = messageRepo.findAll();
            List<Map<String, Object>> response = messages.stream().map(msg -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", msg.id);
                m.put("channel", msg.channel);
                m.put("to", msg.to);
                m.put("subject", msg.subject);
                m.put("body", msg.body);
                return m;
            }).toList();
            ctx.json(response);
        });
    }
}
