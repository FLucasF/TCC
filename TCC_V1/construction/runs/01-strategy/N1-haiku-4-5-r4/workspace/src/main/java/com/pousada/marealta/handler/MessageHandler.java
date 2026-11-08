package com.pousada.marealta.handler;

import com.pousada.marealta.model.Message;
import com.pousada.marealta.model.MessageDto;
import com.pousada.marealta.persistence.MessageDao;
import io.javalin.http.Context;

import java.util.List;

public class MessageHandler {
    private final MessageDao messageDao;

    public MessageHandler(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    public void listMessages(Context ctx) {
        try {
            List<Message> messages = messageDao.findAll();
            ctx.json(messages.stream().map(MessageDto::new).toList());
        } catch (Exception e) {
            ErrorHandler.handleException(ctx, e);
        }
    }
}
