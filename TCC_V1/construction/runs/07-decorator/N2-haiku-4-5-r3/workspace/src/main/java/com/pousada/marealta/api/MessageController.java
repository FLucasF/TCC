package com.pousada.marealta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.infrastructure.MessageRepository;
import io.javalin.http.Context;
import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) throws Exception {
        List<Message> messages = messageRepository.findAll();
        JsonArray jsonArray = new JsonArray();

        for (Message msg : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", msg.getId());
            obj.addProperty("channel", msg.getChannel());
            obj.addProperty("to", msg.getTo());
            obj.addProperty("subject", msg.getSubject());
            obj.addProperty("body", msg.getBody());
            obj.addProperty("status", msg.getStatus().name());
            jsonArray.add(obj);
        }

        ctx.json(jsonArray);
    }
}
