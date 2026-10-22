package com.pousadamarealta.messaging;

import com.pousadamarealta.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public final class MessageService {
    private final MessageRepository messageRepository;
    private final MessageProcessor messageProcessor;

    public MessageService(MessageRepository messageRepository, MessageProcessor messageProcessor) {
        this.messageRepository = messageRepository;
        this.messageProcessor = messageProcessor;
    }

    public void send(OutgoingMessage message) {
        MessageProcessor.ProcessedMessages result = messageProcessor.process(message);

        if (result.blocked()) {
            messageRepository.save(result.originalMessage(), MessageStatus.BLOCKED);
        } else {
            messageRepository.save(result.finalMessage(), MessageStatus.SENT);

            for (OutgoingMessage additional : result.additionalMessages()) {
                messageRepository.save(additional, MessageStatus.SENT);
            }
        }
    }

    public List<SentMessage> findAll() {
        return messageRepository.findAll();
    }
}
