package com.pousadamarealta.service;

import com.pousadamarealta.email.EmailMessage;
import com.pousadamarealta.email.EmailRuleChain;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;
import com.pousadamarealta.repository.MessageRepository;

public class MessageService {
    private final MessageRepository messageRepo;
    private final EmailRuleChain emailRuleChain;

    public MessageService(MessageRepository messageRepo, EmailRuleChain emailRuleChain) {
        this.messageRepo = messageRepo;
        this.emailRuleChain = emailRuleChain;
    }

    public void sendEmail(String to, String subject, String body) {
        EmailMessage emailMsg = new EmailMessage(to, subject, body);
        emailRuleChain.process(emailMsg);

        // Save original message
        MessageStatus status = emailMsg.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT;
        messageRepo.save(new Message(0, MessageChannel.EMAIL, emailMsg.getTo(), emailMsg.getSubject(), emailMsg.getBody(), status));

        // Save copies if any
        for (EmailMessage copy : emailRuleChain.getCopies()) {
            messageRepo.save(new Message(0, MessageChannel.EMAIL, copy.getTo(), copy.getSubject(), copy.getBody(), MessageStatus.SENT));
        }
    }
}
