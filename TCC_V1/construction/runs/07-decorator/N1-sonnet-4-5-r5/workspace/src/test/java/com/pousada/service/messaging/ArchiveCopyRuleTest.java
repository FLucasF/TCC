package com.pousada.service.messaging;

import com.pousada.domain.MessageChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveCopyRuleTest {

    private final ArchiveCopyRule rule = new ArchiveCopyRule();

    @Test
    void shouldCreateArchiveCopy() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "user@example.com", "Subject", "Body"
        );

        ProcessingResult result = rule.process(message);

        assertEquals(1, result.getAdditionalMessages().size());
        OutgoingMessage copy = result.getAdditionalMessages().get(0);
        assertEquals("arquivo@pousadamarealta.com.br", copy.getTo());
        assertEquals("[Cópia] Subject", copy.getSubject());
        assertEquals("Body", copy.getBody());
    }

    @Test
    void shouldNotCreateCopyForBlockedMessage() {
        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL, "invalid", "Subject", "Body"
        );
        message.block();

        ProcessingResult result = rule.process(message);

        assertEquals(0, result.getAdditionalMessages().size());
    }
}
