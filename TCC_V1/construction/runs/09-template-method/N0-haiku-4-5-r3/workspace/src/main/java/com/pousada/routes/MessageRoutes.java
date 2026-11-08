package com.pousada.routes;

import com.pousada.database.Database;
import com.pousada.dto.MessageDTO;
import com.pousada.services.MessageService;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class MessageRoutes {
    private final MessageService messageService;

    public MessageRoutes(Database db) {
        this.messageService = new MessageService(db);
    }

    public void getMessages(Context ctx) {
        List<MessageDTO> messages = messageService.getAllMessages().stream()
                .map(MessageDTO::new)
                .collect(Collectors.toList());
        ctx.json(messages);
    }
}
