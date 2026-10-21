package com.pousadamareaita.http;

import com.pousadamareaita.http.dto.MessageDto;
import com.pousadamareaita.model.Message;
import com.pousadamareaita.storage.MessageRepository;
import io.javalin.Javalin;
import java.util.ArrayList;
import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void registerRoutes(Javalin app) {
        app.get("/messages", ctx -> {
            List<MessageDto> messages = new ArrayList<>();
            for (Message msg : messageRepository.findAll()) {
                messages.add(new MessageDto(msg));
            }
            ctx.json(messages);
        });
    }
}
