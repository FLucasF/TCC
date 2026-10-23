package com.pousadamarealta;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessagePipeline;
import com.pousadamarealta.messaging.MessageRule;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRedirectRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessagePipelineTest {

    @Test
    void testEmailValidationBlocked() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        MessagePipeline pipeline = new MessagePipeline(rules);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "invalid.email",
            "Subject",
            "Body"
        );

        List<OutgoingMessage> result = pipeline.process(message);

        assertEquals(1, result.size());
        assertEquals(MessageStatus.BLOCKED, result.get(0).getStatus());
        assertEquals("invalid.email", result.get(0).getTo());
    }

    @Test
    void testEmailValidationPassed() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        MessagePipeline pipeline = new MessagePipeline(rules);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "valid@example.com",
            "Subject",
            "Body"
        );

        List<OutgoingMessage> result = pipeline.process(message);

        assertEquals(1, result.size());
        assertEquals(MessageStatus.SENT, result.get(0).getStatus());
    }

    @Test
    void testArchiveCopyRule() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new ArchiveCopyRule());
        MessagePipeline pipeline = new MessagePipeline(rules);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@example.com",
            "Test Subject",
            "Test Body"
        );

        List<OutgoingMessage> result = pipeline.process(message);

        assertEquals(2, result.size());

        assertEquals("user@example.com", result.get(0).getTo());
        assertEquals("Test Subject", result.get(0).getSubject());
        assertEquals(MessageStatus.SENT, result.get(0).getStatus());

        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).getTo());
        assertEquals("[Cópia] Test Subject", result.get(1).getSubject());
        assertEquals("Test Body", result.get(1).getBody());
        assertEquals(MessageStatus.SENT, result.get(1).getStatus());
    }

    @Test
    void testArchiveCopyNotCreatedForBlockedMessage() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new ArchiveCopyRule());
        MessagePipeline pipeline = new MessagePipeline(rules);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "invalid",
            "Subject",
            "Body"
        );

        List<OutgoingMessage> result = pipeline.process(message);

        assertEquals(1, result.size());
        assertEquals(MessageStatus.BLOCKED, result.get(0).getStatus());
    }

    @Test
    void testTestModeRedirect() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new TestModeRedirectRule());
        MessagePipeline pipeline = new MessagePipeline(rules);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "original@example.com",
            "Original Subject",
            "Original Body"
        );

        List<OutgoingMessage> result = pipeline.process(message);

        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).getTo());
        assertEquals("[TESTE] Original Subject", result.get(0).getSubject());
        assertTrue(result.get(0).getBody().startsWith("Destinatário original: original@example.com\n"));
        assertTrue(result.get(0).getBody().contains("Original Body"));
        assertEquals(MessageStatus.SENT, result.get(0).getStatus());
    }

    @Test
    void testTestModeDoesNotRedirectBlockedMessage() {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new TestModeRedirectRule());
        MessagePipeline pipeline = new MessagePipeline(rules);

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "invalid.email",
            "Subject",
            "Body"
        );

        List<OutgoingMessage> result = pipeline.process(message);

        assertEquals(1, result.size());
        assertEquals("invalid.email", result.get(0).getTo());
        assertEquals(MessageStatus.BLOCKED, result.get(0).getStatus());
    }
}
