package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.infrastructure.MessageRepository;
import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final boolean testMode;
    private final boolean copyToArchive;

    public MessageService(MessageRepository messageRepository, boolean testMode, boolean copyToArchive) {
        this.messageRepository = messageRepository;
        this.testMode = testMode;
        this.copyToArchive = copyToArchive;
    }

    public void sendAndStoreMessage(Message message) throws Exception {
        MessageProcessingContext context = new MessageProcessingContext(testMode, copyToArchive);

        EmailValidationProcessor emailValidator = new EmailValidationProcessor();
        TestModeProcessor testModeProcessor = new TestModeProcessor();
        ArchiveProcessor archiveProcessor = new ArchiveProcessor();

        emailValidator.setNext(testModeProcessor);
        testModeProcessor.setNext(archiveProcessor);

        MessageProcessingChain chain = new MessageProcessingChain(emailValidator, context);
        List<Message> messages = chain.processMessage(message);
        messageRepository.saveAll(messages);
    }
}
