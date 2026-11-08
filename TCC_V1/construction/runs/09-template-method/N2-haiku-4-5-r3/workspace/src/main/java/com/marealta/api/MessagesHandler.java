package com.marealta.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.marealta.db.MessageRepository;
import com.marealta.model.Message;
import io.javalin.http.Context;

import java.util.List;

public class MessagesHandler {
    private final MessageRepository messageRepository;
    private final Gson gson;

    public MessagesHandler(MessageRepository messageRepository, Gson gson) {
        this.messageRepository = messageRepository;
        this.gson = gson;
    }

    public void listMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        JsonArray jsonArray = new JsonArray();

        for (Message msg : messages) {
            JsonObject json = new JsonObject();
            json.addProperty("id", msg.getId());
            json.addProperty("channel", msg.getChannel());
            json.addProperty("to", msg.getTo());
            json.addProperty("subject", msg.getSubject());
            json.addProperty("body", msg.getBody());
            jsonArray.add(json);
        }

        ctx.json(jsonArray);
    }
}
