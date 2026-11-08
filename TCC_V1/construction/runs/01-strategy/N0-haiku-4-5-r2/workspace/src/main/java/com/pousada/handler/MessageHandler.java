package com.pousada.handler;

import com.pousada.service.MessageService;
import io.javalin.http.Context;

public class MessageHandler {
    public static void listMessages(Context ctx) {
        try {
            ctx.json(MessageService.getAllMessages());
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new com.pousada.util.ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        }
    }
}
