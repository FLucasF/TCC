package com.pousada.api;

import com.google.gson.JsonObject;
import com.pousada.database.MessageRepository;
import com.pousada.model.Message;
import io.javalin.http.Context;
import java.util.List;
import java.util.stream.Collectors;

public class MessagesEndpoint {
  public static void listMessages(Context ctx) {
    List<Message> messages = MessageRepository.findAll();
    List<JsonObject> messagesJson =
        messages.stream()
            .map(
                msg -> {
                  JsonObject obj = new JsonObject();
                  obj.addProperty("id", msg.id);
                  obj.addProperty("channel", msg.channel);
                  obj.addProperty("to", msg.to);
                  obj.addProperty("subject", msg.subject);
                  obj.addProperty("body", msg.body);
                  return obj;
                })
            .collect(Collectors.toList());

    ctx.json(messagesJson);
  }
}
