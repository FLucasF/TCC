package com.pousadamarealta.routes;

import io.javalin.http.Context;
import com.pousadamarealta.repository.MessageRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessageRoutes {
    private final MessageRepository messageRepository = new MessageRepository();

    public void listMessages(Context ctx) {
        try {
            var messages = messageRepository.findAll();
            var response = new ArrayList<>();

            for (var msg : messages) {
                var msgMap = new HashMap<String, Object>();
                msgMap.put("id", msg.id);
                msgMap.put("channel", msg.channel);
                msgMap.put("to", msg.to);
                msgMap.put("subject", msg.subject);
                msgMap.put("body", msg.body);
                response.add(msgMap);
            }

            ctx.json(response);
        } catch (Exception e) {
            ctx.status(400).json(new ErrorResponse("ERROR", e.getMessage()));
        }
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
