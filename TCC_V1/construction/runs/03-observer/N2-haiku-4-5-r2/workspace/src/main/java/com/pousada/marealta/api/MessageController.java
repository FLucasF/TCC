package com.pousada.marealta.api;

import com.pousada.marealta.repository.MessageRepository;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageController {
  private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

  public static void getMessages(Context ctx) {
    try {
      var messages = MessageRepository.findAll();
      var result = messages.stream().map(msg -> new Object() {
        public long id = msg.getId();
        public String channel = msg.getChannel();
        public String to = msg.getTo();
        public String subject = msg.getSubject();
        public String body = msg.getBody();
      }).toList();
      ctx.json(result);
    } catch (Exception e) {
      logger.error("Error fetching messages", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to fetch messages"));
    }
  }
}
