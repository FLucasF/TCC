package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.*;
import com.pousadamarealta.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessagePipeline pipeline;

    public MessageService(MessageRepository messageRepository, boolean isTestMode) {
        this.messageRepository = messageRepository;
        this.pipeline = createPipeline(isTestMode);
    }

    private MessagePipeline createPipeline(boolean isTestMode) {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());

        if (isTestMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessagePipeline(rules);
    }

    public void sendMessage(MessageChannel channel, String to, String subject, String body) {
        OutboundMessage outbound = new OutboundMessage(channel, to, subject, body);
        List<OutboundMessage> processedMessages = pipeline.process(outbound);

        for (OutboundMessage msg : processedMessages) {
            MessageStatus status = msg.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT;
            Message message = new Message(
                messageRepository.nextId(),
                msg.getChannel(),
                msg.getTo(),
                msg.getSubject(),
                msg.getBody(),
                status
            );
            messageRepository.save(message);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
