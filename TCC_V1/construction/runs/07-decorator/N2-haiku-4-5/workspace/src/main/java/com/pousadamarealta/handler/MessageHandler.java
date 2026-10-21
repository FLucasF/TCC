package com.pousadamarealta.handler;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessageHandler {
    private final MessageRepository msgRepo;

    public MessageHandler(MessageRepository msgRepo) {
        this.msgRepo = msgRepo;
    }

    public void listMessages(Context ctx) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Message msg : msgRepo.findAll()) {
            Map<String, Object> msgMap = new LinkedHashMap<>();
            msgMap.put("id", msg.id);
            msgMap.put("channel", msg.channel.toString());
            msgMap.put("to", msg.to);
            msgMap.put("subject", msg.subject);
            msgMap.put("body", msg.body);
            msgMap.put("status", msg.status.toString());
            messages.add(msgMap);
        }
        ctx.json(messages);
    }
}
