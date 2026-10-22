package com.pousada.marealta.http;

import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;

public class MessageHandler {
    private final MessageService messageService;

    public MessageHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    public void listMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<JsonObject> response = messages.stream()
            .map(this::toJson)
            .toList();
        ctx.json(response);
    }

    private JsonObject toJson(Message message) {
        JsonObject json = new JsonObject();
        json.addProperty("id", message.getId());
        json.addProperty("channel", message.getChannel().name());
        json.addProperty("to", message.getTo());
        json.addProperty("subject", message.getSubject());
        json.addProperty("body", message.getBody());
        json.addProperty("status", message.getStatus().name());
        return json;
    }
}
