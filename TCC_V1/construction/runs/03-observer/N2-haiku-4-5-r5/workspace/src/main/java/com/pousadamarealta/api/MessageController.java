package com.pousadamarealta.api;

import com.pousadamarealta.infrastructure.MessageRepository;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;

public class MessageController {
  public static void listMessages(Context ctx) throws Exception {
    var messages = MessageRepository.findAll();
    var messageDtos = messages.stream()
        .map(msg -> {
          Map<String, Object> dto = new HashMap<>();
          dto.put("id", msg.id);
          dto.put("channel", msg.channel.value);
          dto.put("to", msg.to);
          dto.put("subject", msg.subject);
          dto.put("body", msg.body);
          return dto;
        })
        .toList();
    ctx.json(messageDtos);
  }
}
