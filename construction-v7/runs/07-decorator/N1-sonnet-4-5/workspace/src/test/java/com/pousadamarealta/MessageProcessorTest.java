package com.pousadamarealta;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageProcessor;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.messaging.ProcessedMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessageProcessorTest {

    @Test
    public void testValidEmailInProductionMode() {
        MessageProcessor processor = new MessageProcessor(false);
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "guest@example.com",
            "Test Subject",
            "Test Body"
        );

        List<ProcessedMessage> results = processor.process(message);

        assertEquals(2, results.size());

        ProcessedMessage primary = results.get(0);
        assertEquals("guest@example.com", primary.getTo());
        assertEquals("Test Subject", primary.getSubject());
        assertEquals(MessageStatus.SENT, primary.getStatus());

        ProcessedMessage archive = results.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", archive.getTo());
        assertEquals("[Cópia] Test Subject", archive.getSubject());
        assertEquals(MessageStatus.SENT, archive.getStatus());
    }

    @Test
    public void testInvalidEmailBlocked() {
        MessageProcessor processor = new MessageProcessor(false);
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "invalid-email",
            "Test Subject",
            "Test Body"
        );

        List<ProcessedMessage> results = processor.process(message);

        assertEquals(1, results.size());
        ProcessedMessage blocked = results.get(0);
        assertEquals("invalid-email", blocked.getTo());
        assertEquals(MessageStatus.BLOCKED, blocked.getStatus());
    }

    @Test
    public void testValidEmailInTestMode() {
        MessageProcessor processor = new MessageProcessor(true);
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "guest@example.com",
            "Test Subject",
            "Test Body"
        );

        List<ProcessedMessage> results = processor.process(message);

        assertEquals(1, results.size());

        ProcessedMessage redirected = results.get(0);
        assertEquals("testes@pousadamarealta.com.br", redirected.getTo());
        assertEquals("[TESTE] Test Subject", redirected.getSubject());
        assertTrue(redirected.getBody().startsWith("Destinatário original: guest@example.com\n"));
        assertEquals(MessageStatus.SENT, redirected.getStatus());
    }

    @Test
    public void testInvalidEmailInTestModeStillBlocked() {
        MessageProcessor processor = new MessageProcessor(true);
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "invalid-email",
            "Test Subject",
            "Test Body"
        );

        List<ProcessedMessage> results = processor.process(message);

        assertEquals(1, results.size());
        ProcessedMessage blocked = results.get(0);
        assertEquals("invalid-email", blocked.getTo());
        assertEquals(MessageStatus.BLOCKED, blocked.getStatus());
    }

    @Test
    public void testEmailWithoutAt() {
        MessageProcessor processor = new MessageProcessor(false);
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "ana.gmail.com",
            "Test Subject",
            "Test Body"
        );

        List<ProcessedMessage> results = processor.process(message);
        assertEquals(MessageStatus.BLOCKED, results.get(0).getStatus());
    }

    @Test
    public void testEmailWithMultipleAt() {
        MessageProcessor processor = new MessageProcessor(false);
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "ana@@gmail.com",
            "Test Subject",
            "Test Body"
        );

        List<ProcessedMessage> results = processor.process(message);
        assertEquals(MessageStatus.BLOCKED, results.get(0).getStatus());
    }

    @Test
    public void testEmailWithoutDotInDomain() {
        MessageProcessor processor = new MessageProcessor(false);
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "ana@gmail",
            "Test Subject",
            "Test Body"
        );

        List<ProcessedMessage> results = processor.process(message);
        assertEquals(MessageStatus.BLOCKED, results.get(0).getStatus());
    }
}
