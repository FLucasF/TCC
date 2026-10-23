package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;

import java.util.ArrayList;
import java.util.List;

public class MessageService {

    private final MessageRepository messageRepository;
    private final List<MessageProcessor> processors;
    private final ProcessingContext context;

    public MessageService(MessageRepository messageRepository, boolean isTestMode) {
        this.messageRepository = messageRepository;
        this.context = new ProcessingContext(isTestMode);
        this.processors = buildProcessors();
    }

    private List<MessageProcessor> buildProcessors() {
        List<MessageProcessor> processors = new ArrayList<>();
        processors.add(new EmailValidationRule());
        processors.add(new TestModeRule());
        processors.add(new ArchiveCopyRule());
        return processors;
    }

    public void send(Message message) {
        List<Message> allMessages = new ArrayList<>();
        allMessages.add(message);

        for (MessageProcessor processor : processors) {
            List<Message> messagesToProcess = new ArrayList<>(allMessages);
            allMessages.clear();

            for (Message msg : messagesToProcess) {
                ProcessingResult result = processor.process(msg, context);
                allMessages.add(result.getMessage());
                allMessages.addAll(result.getAdditionalMessages());

                if (!result.shouldContinue()) {
                    break;
                }
            }
        }

        for (Message msg : allMessages) {
            messageRepository.save(msg);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
