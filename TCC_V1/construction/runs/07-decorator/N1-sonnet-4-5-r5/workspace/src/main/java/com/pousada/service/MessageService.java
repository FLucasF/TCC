package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import com.pousada.domain.MessageStatus;
import com.pousada.repository.MessageRepository;
import com.pousada.service.messaging.MessageProcessor;
import com.pousada.service.messaging.OutgoingMessage;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageProcessor messageProcessor;

    public MessageService(MessageRepository messageRepository, MessageProcessor messageProcessor) {
        this.messageRepository = messageRepository;
        this.messageProcessor = messageProcessor;
    }

    public void send(MessageChannel channel, String to, String subject, String body) {
        OutgoingMessage outgoing = new OutgoingMessage(channel, to, subject, body);
        List<OutgoingMessage> processedMessages = messageProcessor.process(outgoing);

        for (OutgoingMessage msg : processedMessages) {
            MessageStatus status = msg.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT;
            Message message = new Message(msg.getChannel(), msg.getTo(), msg.getSubject(), msg.getBody(), status);
            messageRepository.save(message);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
