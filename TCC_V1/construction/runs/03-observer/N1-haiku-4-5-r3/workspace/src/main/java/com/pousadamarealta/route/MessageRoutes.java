package com.pousadamarealta.route;

import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.dto.MessageResponse;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.Javalin;

import java.util.List;

public class MessageRoutes {
    public static void register(Javalin app, MessageRepository messageRepository) {
        app.get("/messages", ctx -> {
            try {
                List<Message> messages = messageRepository.findAll();
                List<MessageResponse> responses = messages.stream().map(msg -> {
                    MessageResponse response = new MessageResponse();
                    response.id = msg.getId();
                    response.channel = msg.getChannel();
                    response.to = msg.getTo();
                    response.subject = msg.getSubject();
                    response.body = msg.getBody();
                    return response;
                }).toList();
                ctx.json(responses);
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            }
        });
    }
}
