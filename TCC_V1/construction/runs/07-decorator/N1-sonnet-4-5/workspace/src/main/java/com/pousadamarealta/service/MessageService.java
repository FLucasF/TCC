package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.MessageRuleContext;
import com.pousadamarealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageProcessor messageProcessor;

    public MessageService(MessageRepository messageRepository, MessageProcessor messageProcessor) {
        this.messageRepository = messageRepository;
        this.messageProcessor = messageProcessor;
    }

    public void send(MessageChannel channel, String to, String subject, String body) {
        MessageRuleContext context = new MessageRuleContext(channel, to, subject, body);
        messageProcessor.process(context);

        Message message = new Message(
                messageRepository.nextId(),
                context.getChannel(),
                context.getTo(),
                context.getSubject(),
                context.getBody(),
                context.getStatus()
        );
        messageRepository.save(message);

        for (MessageRuleContext.AdditionalMessage additional : context.getAdditionalMessages()) {
            Message additionalMsg = new Message(
                    messageRepository.nextId(),
                    channel,
                    additional.getTo(),
                    additional.getSubject(),
                    additional.getBody(),
                    context.getStatus()
            );
            messageRepository.save(additionalMsg);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
