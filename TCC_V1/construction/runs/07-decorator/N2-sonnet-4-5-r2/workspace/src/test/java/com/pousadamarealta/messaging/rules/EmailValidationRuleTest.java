package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.messaging.OutgoingMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidationRuleTest {

    @Test
    void testValidEmail() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@example.com",
            "Test",
            "Body"
        );

        boolean result = rule.apply(message);

        assertTrue(result);
        assertFalse(message.isBlocked());
    }

    @Test
    void testInvalidEmailWithoutAt() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user.example.com",
            "Test",
            "Body"
        );

        boolean result = rule.apply(message);

        assertFalse(result);
        assertTrue(message.isBlocked());
    }

    @Test
    void testInvalidEmailWithoutDot() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@examplecom",
            "Test",
            "Body"
        );

        boolean result = rule.apply(message);

        assertFalse(result);
        assertTrue(message.isBlocked());
    }

    @Test
    void testInvalidEmailWithMultipleAt() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@@example.com",
            "Test",
            "Body"
        );

        boolean result = rule.apply(message);

        assertFalse(result);
        assertTrue(message.isBlocked());
    }
}
