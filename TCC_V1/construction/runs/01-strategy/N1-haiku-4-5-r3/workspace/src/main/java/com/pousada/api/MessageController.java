package com.pousada.api;

import io.javalin.Javalin;
import com.pousada.model.Message;
import com.pousada.repository.MessageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController {
  private static final MessageRepository messageRepository = new MessageRepository();

  public static void routes(Javalin app) {
    app.get("/messages", ctx -> {
      List<Message> messages = messageRepository.findAll();
      ctx.json(messages.stream()
          .map(msg -> Map.of(
              "id", msg.getId(),
              "channel", msg.getChannel().toString(),
              "to", msg.getTo(),
              "subject", msg.getSubject(),
              "body", msg.getBody()
          ))
          .toList());
    });
  }
}
