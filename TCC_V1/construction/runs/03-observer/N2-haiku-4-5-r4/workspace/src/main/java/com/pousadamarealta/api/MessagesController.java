package com.pousadamarealta.api;

import com.google.gson.JsonObject;
import com.pousadamarealta.services.MessageService;
import io.javalin.Javalin;
import java.util.stream.Collectors;

public class MessagesController {
    public static void register(Javalin app, MessageService messageService) {
        app.get("/messages", ctx -> {
            var messages = messageService.getAllMessages();
            var json = messages.stream().map(msg -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", msg.getId());
                obj.addProperty("channel", msg.getChannel().getValue());
                obj.addProperty("to", msg.getTo());
                obj.addProperty("subject", msg.getSubject());
                obj.addProperty("body", msg.getBody());
                return obj;
            }).collect(Collectors.toList());
            ctx.json(json);
        });
    }
}
