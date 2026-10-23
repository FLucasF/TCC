package com.pousada.service.messaging;

import com.pousada.domain.MessageChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidationRuleTest {

    private final EmailValidationRule rule = new EmailValidationRule();

    @Test
    void shouldAllowValidEmail() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@example.com", "Subject", "Body"
        );

        rule.process(message);

        assertFalse(message.isBlocked());
    }

    @Test
    void shouldBlockEmailWithoutAt() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "invalid.email", "Subject", "Body"
        );

        rule.process(message);

        assertTrue(message.isBlocked());
    }

    @Test
    void shouldBlockEmailWithMultipleAt() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@@example.com", "Subject", "Body"
        );

        rule.process(message);

        assertTrue(message.isBlocked());
    }

    @Test
    void shouldBlockEmailWithoutDomainDot() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@example", "Subject", "Body"
        );

        rule.process(message);

        assertTrue(message.isBlocked());
    }

    @Test
    void shouldBlockEmailStartingWithAt() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "@example.com", "Subject", "Body"
        );

        rule.process(message);

        assertTrue(message.isBlocked());
    }

    @Test
    void shouldBlockEmailEndingWithAt() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@", "Subject", "Body"
        );

        rule.process(message);

        assertTrue(message.isBlocked());
    }
}
