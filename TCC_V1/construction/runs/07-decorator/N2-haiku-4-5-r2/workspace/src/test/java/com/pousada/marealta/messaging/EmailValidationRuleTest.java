package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidationRuleTest {
    @Test
    void shouldBlockInvalidEmail() {
        Message message = new Message(Message.Channel.EMAIL, "invalid-email", "Subject", "Body");
        MessageProcessingContext context = new MessageProcessingContext(new ArrayList<>());

        EmailValidationRule rule = new EmailValidationRule();
        rule.process(message, context);

        assertEquals(Message.Status.BLOCKED, message.getStatus());
    }

    @Test
    void shouldNotBlockValidEmail() {
        Message message = new Message(Message.Channel.EMAIL, "user@example.com", "Subject", "Body");
        MessageProcessingContext context = new MessageProcessingContext(new ArrayList<>());

        EmailValidationRule rule = new EmailValidationRule();
        rule.process(message, context);

        assertEquals(Message.Status.SENT, message.getStatus());
    }
}
