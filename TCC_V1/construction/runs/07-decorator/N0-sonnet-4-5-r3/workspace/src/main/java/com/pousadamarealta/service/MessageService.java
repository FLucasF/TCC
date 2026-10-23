package com.pousadamarealta.service;

import com.pousadamarealta.messaging.*;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageStatus;
import com.pousadamarealta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessagePipeline pipeline;

    public MessageService(MessageRepository messageRepository, boolean testMode) {
        this.messageRepository = messageRepository;
        this.pipeline = buildPipeline(testMode);
    }

    private MessagePipeline buildPipeline(boolean testMode) {
        MessagePipeline.Builder builder = new MessagePipeline.Builder();

        builder.addRule(new EmailValidationRule());

        if (testMode) {
            builder.addRule(new TestModeRule());
        } else {
            builder.addRule(new ArchiveCopyRule());
        }

        return builder.build();
    }

    public void send(OutgoingMessage outgoingMessage) {
        ProcessingResult result = pipeline.process(outgoingMessage);

        OutgoingMessage processedMessage = result.getMessage();
        MessageStatus status = result.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT;

        Message message = new Message(
            processedMessage.getChannel(),
            processedMessage.getTo(),
            processedMessage.getSubject(),
            processedMessage.getBody(),
            status
        );
        messageRepository.save(message);

        if (!result.isBlocked()) {
            for (OutgoingMessage additionalMessage : result.getAdditionalMessages()) {
                Message additional = new Message(
                    additionalMessage.getChannel(),
                    additionalMessage.getTo(),
                    additionalMessage.getSubject(),
                    additionalMessage.getBody(),
                    MessageStatus.SENT
                );
                messageRepository.save(additional);
            }
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
