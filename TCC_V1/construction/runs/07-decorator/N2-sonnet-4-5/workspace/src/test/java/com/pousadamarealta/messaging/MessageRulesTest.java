package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageRulesTest {

    @Test
    void emailValidationShouldBlockInvalidEmails() {
        EmailValidationRule rule = new EmailValidationRule();

        Message valid = new Message(1L, "EMAIL", "ana@gmail.com", "Test", "Body", MessageStatus.SENT);
        Message noAt = new Message(2L, "EMAIL", "ana.gmail.com", "Test", "Body", MessageStatus.SENT);
        Message noDomain = new Message(3L, "EMAIL", "ana@gmail", "Test", "Body", MessageStatus.SENT);

        List<Message> result = rule.process(List.of(valid, noAt, noDomain));

        assertEquals(MessageStatus.SENT, result.get(0).status());
        assertEquals(MessageStatus.BLOCKED, result.get(1).status());
        assertEquals(MessageStatus.BLOCKED, result.get(2).status());
    }

    @Test
    void archiveCopyShouldDuplicateSentMessages() {
        ArchiveCopyRule rule = new ArchiveCopyRule("arquivo@pousadamarealta.com.br", 100L);

        Message sent = new Message(1L, "EMAIL", "guest@example.com", "Subject", "Body", MessageStatus.SENT);
        Message blocked = new Message(2L, "EMAIL", "invalid", "Subject", "Body", MessageStatus.BLOCKED);

        List<Message> result = rule.process(List.of(sent, blocked));

        assertEquals(3, result.size());
        assertEquals("guest@example.com", result.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).to());
        assertEquals("[Cópia] Subject", result.get(1).subject());
        assertEquals("invalid", result.get(2).to());
    }

    @Test
    void testModeShouldRedirectToTestEmail() {
        TestModeRule rule = new TestModeRule("testes@pousadamarealta.com.br");

        Message original = new Message(1L, "EMAIL", "guest@example.com", "Subject", "Body", MessageStatus.SENT);

        List<Message> result = rule.process(List.of(original));

        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).to());
        assertEquals("[TESTE] Subject", result.get(0).subject());
        assertTrue(result.get(0).body().startsWith("Destinatário original: guest@example.com\n"));
    }

    @Test
    void messageProcessorShouldApplyRulesInOrder() {
        MessageProcessor processor = new MessageProcessor(List.of(
            new TestModeRule("testes@pousadamarealta.com.br"),
            new EmailValidationRule()
        ));

        Message original = new Message(1L, "EMAIL", "guest@example.com", "Subject", "Body", MessageStatus.SENT);

        List<Message> result = processor.process(List.of(original));

        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).to());
        assertEquals("[TESTE] Subject", result.get(0).subject());
    }

    @Test
    void productionModeShouldValidateAndCopy() {
        MessageProcessor processor = new MessageProcessor(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule("arquivo@pousadamarealta.com.br", 100L)
        ));

        Message valid = new Message(1L, "EMAIL", "guest@example.com", "Subject", "Body", MessageStatus.SENT);
        Message invalid = new Message(2L, "EMAIL", "invalid", "Subject", "Body", MessageStatus.SENT);

        List<Message> result = processor.process(List.of(valid, invalid));

        assertEquals(3, result.size());
        assertEquals("guest@example.com", result.get(0).to());
        assertEquals(MessageStatus.SENT, result.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).to());
        assertEquals(MessageStatus.SENT, result.get(1).status());
        assertEquals("invalid", result.get(2).to());
        assertEquals(MessageStatus.BLOCKED, result.get(2).status());
    }
}
