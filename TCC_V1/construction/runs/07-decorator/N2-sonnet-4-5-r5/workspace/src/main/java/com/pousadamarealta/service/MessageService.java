package com.pousadamarealta.service;

import com.pousadamarealta.database.MessageDao;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.messaging.MessagePipeline;
import com.pousadamarealta.messaging.OutgoingMessage;

import java.util.List;

public class MessageService {
    private final MessageDao messageDao;
    private final MessagePipeline pipeline;

    public MessageService(MessageDao messageDao, MessagePipeline pipeline) {
        this.messageDao = messageDao;
        this.pipeline = pipeline;
    }

    public void send(String to, String subject, String body) {
        OutgoingMessage outgoing = new OutgoingMessage(MessageChannel.EMAIL, to, subject, body);
        List<OutgoingMessage> processedMessages = pipeline.process(outgoing);

        for (OutgoingMessage msg : processedMessages) {
            Message message = new Message(
                null,
                msg.getChannel(),
                msg.getTo(),
                msg.getSubject(),
                msg.getBody(),
                msg.getStatus()
            );
            messageDao.create(message);
        }
    }

    public List<Message> getAllMessages() {
        return messageDao.findAll();
    }
}
