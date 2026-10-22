package com.mareaalta.api;

import com.google.gson.JsonObject;
import com.mareaalta.domain.Message;
import com.mareaalta.infrastructure.MessageRepository;
import io.javalin.http.Context;
import java.util.*;

public class MessageController {
    public static void getMessages(Context ctx) throws Exception {
        List<Message> messages = MessageRepository.findAll();

        List<JsonObject> messagesJson = new ArrayList<>();
        for (Message msg : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", msg.getId());
            obj.addProperty("channel", msg.getChannel());
            obj.addProperty("to", msg.getTo());
            obj.addProperty("subject", msg.getSubject());
            obj.addProperty("body", msg.getBody());
            messagesJson.add(obj);
        }

        ctx.json(messagesJson);
    }
}
