package com.pousada.service.messaging;

import com.pousada.domain.MessageChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestModeRuleTest {

    private final TestModeRule rule = new TestModeRule();

    @Test
    void shouldRedirectToTestEmail() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@example.com", "Subject", "Body"
        );

        rule.process(message);

        assertEquals("testes@pousadamarealta.com.br", message.getTo());
        assertTrue(message.getSubject().startsWith("[TESTE] "));
        assertTrue(message.getBody().startsWith("Destinatário original: user@example.com"));
    }

    @Test
    void shouldNotModifyBlockedMessage() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "invalid", "Subject", "Body"
        );
        message.block();

        String originalTo = message.getTo();
        String originalSubject = message.getSubject();
        String originalBody = message.getBody();

        rule.process(message);

        assertEquals(originalTo, message.getTo());
        assertEquals(originalSubject, message.getSubject());
        assertEquals(originalBody, message.getBody());
    }
}
