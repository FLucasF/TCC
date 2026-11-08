package com.pousada;

import com.pousada.api.dto.MessageResponse;
import com.pousada.infrastructure.MessageRepository;
import io.javalin.http.Context;

public class MessagesHandler {
    private final MessageRepository messageRepository = new MessageRepository();

    public void list(Context ctx) {
        try {
            var messages = messageRepository.findAll();
            var response = messages.stream().map(MessageResponse::fromDomain).toList();
            ctx.json(response);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        }
    }

    static class ErrorResponse {
        String error;
        String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
