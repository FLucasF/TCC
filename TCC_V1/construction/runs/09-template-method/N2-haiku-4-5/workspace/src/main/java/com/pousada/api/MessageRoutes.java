package com.pousada.api;

import com.pousada.domain.SentMessage;
import com.pousada.repo.MessageRepository;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class MessageRoutes {
    public static void register(Javalin app, MessageRepository messageRepo) {
        app.get("/messages", ctx -> {
            try {
                List<SentMessage> messages = messageRepo.findAll();
                JSONArray array = new JSONArray();
                for (SentMessage msg : messages) {
                    array.put(new JSONObject()
                        .put("id", msg.getId())
                        .put("channel", msg.getChannel().getDisplayName())
                        .put("to", msg.getTo())
                        .put("subject", msg.getSubject())
                        .put("body", msg.getBody())
                    );
                }
                ctx.json(array.toString());
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new JSONObject()
                    .put("error", "INTERNAL_ERROR")
                    .put("message", e.getMessage())
                    .toString());
            }
        });
    }
}
