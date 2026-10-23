package com.pousadamarealta;

import com.pousadamarealta.messaging.*;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;
import com.pousadamarealta.model.MessageChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageRulesTest {

    @Test
    void emailValidationShouldBlockInvalidEmails() {
        EmailValidationRule rule = new EmailValidationRule();

        OutgoingMessage invalidEmail1 = new OutgoingMessage(MessageChannel.EMAIL, "ana@gmail", "Test", "Body");
        ProcessingResult result1 = rule.process(invalidEmail1);
        assertTrue(result1.isBlocked());

        OutgoingMessage invalidEmail2 = new OutgoingMessage(MessageChannel.EMAIL, "ana.gmail.com", "Test", "Body");
        ProcessingResult result2 = rule.process(invalidEmail2);
        assertTrue(result2.isBlocked());

        OutgoingMessage invalidEmail3 = new OutgoingMessage(MessageChannel.EMAIL, "@gmail.com", "Test", "Body");
        ProcessingResult result3 = rule.process(invalidEmail3);
        assertTrue(result3.isBlocked());

        OutgoingMessage invalidEmail4 = new OutgoingMessage(MessageChannel.EMAIL, "ana@", "Test", "Body");
        ProcessingResult result4 = rule.process(invalidEmail4);
        assertTrue(result4.isBlocked());
    }

    @Test
    void emailValidationShouldAllowValidEmails() {
        EmailValidationRule rule = new EmailValidationRule();

        OutgoingMessage validEmail = new OutgoingMessage(MessageChannel.EMAIL, "ana@gmail.com", "Test", "Body");
        ProcessingResult result = rule.process(validEmail);
        assertFalse(result.isBlocked());
    }

    @Test
    void testModeShouldRedirectToTestAddress() {
        TestModeRule rule = new TestModeRule();

        OutgoingMessage message = new OutgoingMessage(MessageChannel.EMAIL, "guest@example.com", "Subject", "Body");
        ProcessingResult result = rule.process(message);

        assertFalse(result.isBlocked());
        assertEquals("testes@pousadamarealta.com.br", message.getTo());
        assertTrue(message.getSubject().startsWith("[TESTE] "));
        assertTrue(message.getBody().startsWith("Destinatário original: guest@example.com\n"));
    }

    @Test
    void archiveCopyShouldCreateAdditionalMessage() {
        ArchiveCopyRule rule = new ArchiveCopyRule();

        OutgoingMessage message = new OutgoingMessage(MessageChannel.EMAIL, "guest@example.com", "Subject", "Body");
        ProcessingResult result = rule.process(message);

        assertFalse(result.isBlocked());
        assertEquals(1, result.getAdditionalMessages().size());

        OutgoingMessage copy = result.getAdditionalMessages().get(0);
        assertEquals("arquivo@pousadamarealta.com.br", copy.getTo());
        assertEquals("[Cópia] Subject", copy.getSubject());
        assertEquals("Body", copy.getBody());
    }

    @Test
    void pipelineShouldApplyRulesInOrder() {
        MessagePipeline pipeline = new MessagePipeline.Builder()
            .addRule(new EmailValidationRule())
            .addRule(new ArchiveCopyRule())
            .build();

        OutgoingMessage validMessage = new OutgoingMessage(MessageChannel.EMAIL, "guest@example.com", "Subject", "Body");
        ProcessingResult validResult = pipeline.process(validMessage);

        assertFalse(validResult.isBlocked());
        assertEquals(1, validResult.getAdditionalMessages().size());

        OutgoingMessage invalidMessage = new OutgoingMessage(MessageChannel.EMAIL, "invalid@", "Subject", "Body");
        ProcessingResult invalidResult = pipeline.process(invalidMessage);

        assertTrue(invalidResult.isBlocked());
        assertEquals(0, invalidResult.getAdditionalMessages().size());
    }

    @Test
    void testModePipelineShouldNotCreateArchiveCopy() {
        MessagePipeline pipeline = new MessagePipeline.Builder()
            .addRule(new EmailValidationRule())
            .addRule(new TestModeRule())
            .build();

        OutgoingMessage message = new OutgoingMessage(MessageChannel.EMAIL, "guest@example.com", "Subject", "Body");
        ProcessingResult result = pipeline.process(message);

        assertFalse(result.isBlocked());
        assertEquals(0, result.getAdditionalMessages().size());
        assertEquals("testes@pousadamarealta.com.br", message.getTo());
    }
}
