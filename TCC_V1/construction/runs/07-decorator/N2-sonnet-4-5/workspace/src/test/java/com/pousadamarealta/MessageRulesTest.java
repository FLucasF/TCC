package com.pousadamarealta;

import com.pousadamarealta.messaging.MessageChannel;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessingResult;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageRulesTest {
    @Test
    void testEmailValidationAcceptsValidEmail() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@example.com",
            "Test",
            "Body"
        );

        ProcessingResult result = rule.process(message);
        assertFalse(result.isBlocked());
    }

    @Test
    void testEmailValidationBlocksInvalidEmail() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user.example.com",
            "Test",
            "Body"
        );

        ProcessingResult result = rule.process(message);
        assertTrue(result.isBlocked());
    }

    @Test
    void testTestModeModifiesMessage() {
        TestModeRule rule = new TestModeRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@example.com",
            "Test Subject",
            "Test Body"
        );

        ProcessingResult result = rule.process(message);
        assertFalse(result.isBlocked());
        assertEquals("testes@pousadamarealta.com.br", result.processedMessage().to());
        assertEquals("[TESTE] Test Subject", result.processedMessage().subject());
        assertTrue(result.processedMessage().body().startsWith("Destinatário original: user@example.com"));
    }

    @Test
    void testArchiveCopyGeneratesAdditionalMessage() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@example.com",
            "Test Subject",
            "Test Body"
        );

        ProcessingResult result = rule.process(message);
        assertFalse(result.isBlocked());
        assertEquals(1, result.additionalMessages().size());

        OutgoingMessage copy = result.additionalMessages().get(0);
        assertEquals("arquivo@pousadamarealta.com.br", copy.to());
        assertEquals("[Cópia] Test Subject", copy.subject());
        assertEquals("Test Body", copy.body());
    }
}
