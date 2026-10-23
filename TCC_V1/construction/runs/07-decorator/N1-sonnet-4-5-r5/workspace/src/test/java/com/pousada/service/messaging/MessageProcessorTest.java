package com.pousada.service.messaging;

import com.pousada.domain.MessageChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageProcessorTest {

    @Test
    void shouldApplyRulesInOrder() {
        MessageProcessor processor = new MessageProcessor(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@example.com", "Subject", "Body"
        );

        List<OutgoingMessage> result = processor.process(message);

        assertEquals(2, result.size());
        assertEquals("user@example.com", result.get(0).getTo());
        assertFalse(result.get(0).isBlocked());
        assertEquals("arquivo@pousadamarealta.com.br", result.get(1).getTo());
    }

    @Test
    void shouldNotCreateCopyWhenEmailIsInvalid() {
        MessageProcessor processor = new MessageProcessor(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "invalid.email", "Subject", "Body"
        );

        List<OutgoingMessage> result = processor.process(message);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isBlocked());
    }

    @Test
    void shouldApplyTestModeCorrectly() {
        MessageProcessor processor = new MessageProcessor(List.of(
            new EmailValidationRule(),
            new TestModeRule()
        ));

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@example.com", "Subject", "Body"
        );

        List<OutgoingMessage> result = processor.process(message);

        assertEquals(1, result.size());
        assertEquals("testes@pousadamarealta.com.br", result.get(0).getTo());
        assertTrue(result.get(0).getSubject().startsWith("[TESTE] "));
    }
}
