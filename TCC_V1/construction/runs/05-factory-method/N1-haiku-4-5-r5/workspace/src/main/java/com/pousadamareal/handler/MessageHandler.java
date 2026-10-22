package com.pousadamareal.handler;

import com.pousadamareal.model.Message;
import com.pousadamareal.repository.MessageRepository;
import com.pousadamareal.util.JsonUtils;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;

public class MessageHandler {

    public static void listMessages(Context ctx) {
        try {
            List<Message> messages = MessageRepository.findAll();
            ctx.json(messages.stream().map(msg -> JsonUtils.map(
                "id", msg.id(),
                "channel", msg.channel(),
                "to", msg.to(),
                "subject", msg.subject(),
                "body", msg.body()
            )).toList());
        } catch (SQLException e) {
            ctx.status(500).json(JsonUtils.map("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }
}
