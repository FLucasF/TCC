package com.pousada.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.model.Message;
import com.pousada.repository.MessageRepository;
import io.javalin.http.Context;
import java.util.List;

public class MessageController {
    public static void getMessages(Context ctx) {
        List<Message> messages = MessageRepository.findAll();

        JsonArray array = new JsonArray();
        for (Message msg : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", msg.getId());
            obj.addProperty("channel", msg.getChannel());
            obj.addProperty("to", msg.getTo());
            obj.addProperty("subject", msg.getSubject());
            obj.addProperty("body", msg.getBody());
            array.add(obj);
        }

        ctx.json(array);
    }
}
