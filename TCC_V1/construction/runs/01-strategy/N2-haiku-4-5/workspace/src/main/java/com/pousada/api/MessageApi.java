package com.pousada.api;

import com.pousada.domain.Message;
import com.pousada.service.MessageService;
import com.pousada.repository.MessageRepository;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageApi {
    private static final Logger log = LoggerFactory.getLogger(MessageApi.class);
    private static final MessageRepository messageRepo = new MessageRepository();
    private static final MessageService messageService = new MessageService(messageRepo);

    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                var messages = messageService.getAllMessages();
                JSONArray json = new JSONArray();

                for (Message msg : messages) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", msg.getId());
                    obj.put("channel", msg.getChannel().name());
                    obj.put("to", msg.getTo());
                    obj.put("subject", msg.getSubject());
                    obj.put("body", msg.getBody());
                    json.put(obj);
                }

                ctx.json(json);
            } catch (Exception e) {
                log.error("Error retrieving messages", e);
                ctx.status(500).json(new JSONObject().put("error", "INTERNAL_ERROR"));
            }
        });
    }
}
