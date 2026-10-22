package com.pousadamareal.handlers;

import com.pousadamareal.dao.MessageDAO;
import com.pousadamareal.model.Message;
import io.javalin.http.Context;

import java.util.List;

public class MessageHandler {
    public static void getMessages(Context ctx) {
        try {
            List<Message> messages = MessageDAO.getAllMessages();
            ctx.json(messages);
        } catch (Exception e) {
            ctx.json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
            ctx.status(500);
        }
    }

    public record ErrorResponse(String error, String message) {}
}
