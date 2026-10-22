package com.pousadamareaita.api;

import io.javalin.http.Context;
import com.pousadamareaita.database.Database;
import com.pousadamareaita.service.MessageService;

public class MessageHandler {
    private final MessageService messageService;

    public MessageHandler(Database db) {
        this.messageService = new MessageService(db);
    }

    public void getMessages(Context ctx) throws Exception {
        ctx.json(messageService.getMessages());
    }
}
