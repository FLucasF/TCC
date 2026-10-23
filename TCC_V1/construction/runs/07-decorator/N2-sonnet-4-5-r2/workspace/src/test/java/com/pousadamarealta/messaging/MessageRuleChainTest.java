package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageRuleChainTest {

    @Test
    void testChainWithValidEmail() {
        MessageRuleChain chain = new MessageRuleChain(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@example.com",
            "Test",
            "Body"
        );

        chain.process(message);

        assertEquals(MessageStatus.SENT, message.getStatus());
        assertEquals(1, message.getAdditionalMessages().size());
        assertEquals("arquivo@pousadamarealta.com.br", message.getAdditionalMessages().get(0).getTo());
    }

    @Test
    void testChainWithInvalidEmail() {
        MessageRuleChain chain = new MessageRuleChain(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "invalid-email",
            "Test",
            "Body"
        );

        chain.process(message);

        assertEquals(MessageStatus.BLOCKED, message.getStatus());
        assertEquals(0, message.getAdditionalMessages().size());
    }

    @Test
    void testChainWithTestMode() {
        MessageRuleChain chain = new MessageRuleChain(List.of(
            new EmailValidationRule(),
            new TestModeRule()
        ));

        OutgoingMessage message = new OutgoingMessage(
            MessageChannel.EMAIL,
            "user@example.com",
            "Test",
            "Body"
        );

        chain.process(message);

        assertEquals("testes@pousadamarealta.com.br", message.getTo());
        assertTrue(message.getSubject().startsWith("[TESTE] "));
        assertTrue(message.getBody().startsWith("Destinatário original: user@example.com"));
    }
}
