package com.pousada.marealta.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pousada.marealta.domain.Message;
import com.pousada.marealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;

public class MessagesHandler extends Handler {
    private final MessageRepository messageRepository;

    public MessagesHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        try {
            List<Message> messages = messageRepository.getAllMessages();
            JsonArray array = new JsonArray();

            for (Message message : messages) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", message.getId());
                obj.addProperty("channel", message.getChannel());
                obj.addProperty("to", message.getTo());
                obj.addProperty("subject", message.getSubject());
                obj.addProperty("body", message.getBody());
                obj.addProperty("status", message.getStatus().name());
                array.add(obj);
            }

            ctx.json(array);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }
}
