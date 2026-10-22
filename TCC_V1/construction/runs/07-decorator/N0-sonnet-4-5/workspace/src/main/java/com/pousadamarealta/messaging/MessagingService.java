package com.pousadamarealta.messaging;

import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRedirectRule;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;
import com.pousadamarealta.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public class MessagingService {
    private final MessageRepository messageRepository;
    private final List<MessageRule> rules;
    private final boolean testMode;

    public MessagingService(MessageRepository messageRepository, boolean testMode) {
        this.messageRepository = messageRepository;
        this.testMode = testMode;
        this.rules = new ArrayList<>();

        rules.add(new EmailValidationRule());
        rules.add(new TestModeRedirectRule());
        rules.add(new ArchiveCopyRule());
    }

    public void sendEmail(String to, String subject, String body) {
        OutboundMessage outbound = new OutboundMessage(MessageChannel.EMAIL, to, subject, body);
        RuleContext context = new RuleContext(testMode);

        for (MessageRule rule : rules) {
            rule.apply(outbound, context);
        }

        MessageStatus status = outbound.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT;
        Message message = new Message(
            outbound.getChannel(),
            outbound.getTo(),
            outbound.getSubject(),
            outbound.getBody(),
            status
        );
        messageRepository.save(message);

        for (OutboundMessage additional : context.getAdditionalMessages()) {
            Message additionalMessage = new Message(
                additional.getChannel(),
                additional.getTo(),
                additional.getSubject(),
                additional.getBody(),
                MessageStatus.SENT
            );
            messageRepository.save(additionalMessage);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
