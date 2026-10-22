package com.posadamarealta.handlers;

import com.posadamarealta.repository.MessageRepository;
import io.javalin.http.Context;
import javax.sql.DataSource;

public class MessageHandler {
    private final MessageRepository messageRepo;

    public MessageHandler(DataSource ds) {
        this.messageRepo = new MessageRepository(ds);
    }

    public void list(Context ctx) {
        ctx.json(messageRepo.findAll());
    }
}
