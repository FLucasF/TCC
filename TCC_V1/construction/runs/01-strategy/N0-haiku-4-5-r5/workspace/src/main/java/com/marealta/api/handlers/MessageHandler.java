package com.marealta.api.handlers;

import io.javalin.http.Context;
import com.marealta.service.MessageService;
import com.marealta.domain.Message;
import java.util.List;
import java.util.stream.Collectors;

public class MessageHandler {
    public static void list(Context ctx) throws Exception {
        List<Message> messages = MessageService.getAllMessages();

        var response = messages.stream().map(msg -> new Object() {
            public int id = msg.id;
            public String channel = msg.channel;
            public String to = msg.to;
            public String subject = msg.subject;
            public String body = msg.body;
        }).collect(Collectors.toList());

        ctx.json(response);
    }
}
