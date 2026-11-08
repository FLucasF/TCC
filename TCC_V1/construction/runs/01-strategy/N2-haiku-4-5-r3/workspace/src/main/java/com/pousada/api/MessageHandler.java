package com.pousada.api;

import com.pousada.database.MessageDao;
import com.pousada.model.Message;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class MessageHandler {
    public static void list(Context ctx) throws Exception {
        List<Message> messages = MessageDao.findAll();
        ctx.json(messages.stream().map(m -> Map.of(
                "id", m.id,
                "channel", m.channel,
                "to", m.to,
                "subject", m.subject,
                "body", m.body
        )).toList());
    }
}
