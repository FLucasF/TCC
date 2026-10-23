package com.pousadamarealta.messaging;

import com.pousadamarealta.database.MessageDAO;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;

import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final MessageDAO messageDAO;
    private final List<MessageRule> rules;

    public MessageService(MessageDAO messageDAO, boolean testMode) {
        this.messageDAO = messageDAO;
        this.rules = new ArrayList<>();

        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }
    }

    public void sendMessage(MessageChannel channel, String to, String subject, String body) {
        Message message = new Message(channel, to, subject, body, null);

        List<Message> messagesToSave = new ArrayList<>();
        messagesToSave.add(message);

        for (MessageRule rule : rules) {
            List<Message> currentMessages = new ArrayList<>();
            for (Message msg : messagesToSave) {
                currentMessages.addAll(rule.apply(msg));
            }
            messagesToSave = currentMessages;
        }

        for (Message msg : messagesToSave) {
            messageDAO.save(msg);
        }
    }

    public List<Message> getAllMessages() {
        return messageDAO.findAll();
    }
}
