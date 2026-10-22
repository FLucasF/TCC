package com.pousada.api.handler;

import com.pousada.domain.Message;
import com.pousada.repository.MessageRepository;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;

public class MessageHandler {
    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            var messages = MessageRepository.getInstance().findAll();
            JSONArray array = new JSONArray();
            for (Message msg : messages) {
                JSONObject obj = new JSONObject();
                obj.put("id", msg.getId());
                obj.put("channel", msg.getChannel());
                obj.put("to", msg.getTo());
                obj.put("subject", msg.getSubject());
                obj.put("body", msg.getBody());
                array.put(obj);
            }
            ctx.json(array.toList());
        });
    }
}
