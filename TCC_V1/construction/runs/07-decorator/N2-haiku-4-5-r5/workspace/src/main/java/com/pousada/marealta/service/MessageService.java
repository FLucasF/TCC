package com.pousada.marealta.service;

import com.pousada.marealta.config.Config;
import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.messaging.MessageData;
import com.pousada.marealta.messaging.MessageProcessorChain;
import com.pousada.marealta.messaging.processors.ArchiveCopyProcessor;
import com.pousada.marealta.messaging.processors.EmailValidationProcessor;
import com.pousada.marealta.messaging.processors.TestModeProcessor;
import com.pousada.marealta.repository.MessageRepository;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageProcessorChain processorChain;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
        this.processorChain = createProcessorChain();
    }

    public void sendEmail(String to, String subject, String body) throws Exception {
        MessageData message = new MessageData(to, subject, body);
        processorChain.process(message);

        messageRepository.create(
            "EMAIL",
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            message.getStatus()
        );

        if (message.shouldGenerateCopy() && !message.isBlocked()) {
            messageRepository.create(
                "EMAIL",
                ArchiveCopyProcessor.ARCHIVE_EMAIL,
                "[Cópia] " + message.getSubject(),
                message.getBody(),
                MessageStatus.SENT
            );
        }
    }

    private MessageProcessorChain createProcessorChain() {
        MessageProcessorChain chain = new MessageProcessorChain();
        chain.addProcessor(new EmailValidationProcessor(true));
        chain.addProcessor(new TestModeProcessor(Config.isTestMode()));
        chain.addProcessor(new ArchiveCopyProcessor(!Config.isTestMode()));
        return chain;
    }
}
