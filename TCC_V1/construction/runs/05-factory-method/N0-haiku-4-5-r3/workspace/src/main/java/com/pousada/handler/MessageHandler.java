package com.pousada.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.model.Message;
import com.pousada.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageHandler {
    private static final MessageService messageService = new MessageService();

    public static void getMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();

        JsonArray jsonArray = new JsonArray();
        for (Message message : messages) {
            JsonObject json = new JsonObject();
            json.addProperty("id", message.getId());
            json.addProperty("channel", message.getChannel());
            json.addProperty("to", message.getTo());
            json.addProperty("subject", message.getSubject());
            json.addProperty("body", message.getBody());
            jsonArray.add(json);
        }

        ctx.json(jsonArray);
    }
}
