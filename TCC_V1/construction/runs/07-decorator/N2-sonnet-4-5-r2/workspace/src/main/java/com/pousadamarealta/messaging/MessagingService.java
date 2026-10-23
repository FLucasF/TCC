package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;
import com.pousadamarealta.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public final class MessagingService {
    private final MessageRepository messageRepository;
    private final MessageRuleChain ruleChain;

    public MessagingService(MessageRepository messageRepository, boolean testMode) {
        this.messageRepository = messageRepository;
        this.ruleChain = buildRuleChain(testMode);
    }

    private MessageRuleChain buildRuleChain(boolean testMode) {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return new MessageRuleChain(rules);
    }

    public void sendEmail(String to, String subject, String body) {
        OutgoingMessage message = new OutgoingMessage(MessageChannel.EMAIL, to, subject, body);
        ruleChain.process(message);

        messageRepository.save(message.getChannel(), message.getTo(), message.getSubject(),
                              message.getBody(), message.getStatus());

        for (OutgoingMessage additional : message.getAdditionalMessages()) {
            messageRepository.save(additional.getChannel(), additional.getTo(),
                                 additional.getSubject(), additional.getBody(),
                                 additional.getStatus());
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
