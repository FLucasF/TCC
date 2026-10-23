package com.pousadamarealta;

import com.pousadamarealta.messaging.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageRulesTest {

    @Test
    void shouldBlockInvalidEmailWithoutAt() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage("ana.gmail.com", "Test", "Body");
        MessageContext context = new MessageContext(message);

        rule.process(context);

        assertEquals(MessageStatus.BLOCKED, context.getStatus());
    }

    @Test
    void shouldBlockInvalidEmailWithoutDotInDomain() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage("ana@gmail", "Test", "Body");
        MessageContext context = new MessageContext(message);

        rule.process(context);

        assertEquals(MessageStatus.BLOCKED, context.getStatus());
    }

    @Test
    void shouldAllowValidEmail() {
        EmailValidationRule rule = new EmailValidationRule();
        OutgoingMessage message = new OutgoingMessage("ana@gmail.com", "Test", "Body");
        MessageContext context = new MessageContext(message);

        rule.process(context);

        assertEquals(MessageStatus.SENT, context.getStatus());
    }

    @Test
    void shouldRedirectToTestRecipientInTestMode() {
        TestModeRule rule = new TestModeRule();
        OutgoingMessage message = new OutgoingMessage("ana@gmail.com", "Subject", "Body");
        MessageContext context = new MessageContext(message);

        rule.process(context);

        assertEquals("testes@pousadamarealta.com.br", context.getMessage().to());
        assertEquals("[TESTE] Subject", context.getMessage().subject());
        assertEquals("Destinatário original: ana@gmail.com\nBody", context.getMessage().body());
    }

    @Test
    void shouldNotRedirectBlockedMessageInTestMode() {
        TestModeRule rule = new TestModeRule();
        OutgoingMessage message = new OutgoingMessage("ana@gmail.com", "Subject", "Body");
        MessageContext context = new MessageContext(message);
        context.setStatus(MessageStatus.BLOCKED);

        rule.process(context);

        assertEquals("ana@gmail.com", context.getMessage().to());
    }

    @Test
    void shouldCreateArchiveCopy() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        OutgoingMessage message = new OutgoingMessage("ana@gmail.com", "Subject", "Body");
        MessageContext context = new MessageContext(message);

        rule.process(context);

        List<OutgoingMessage> additional = context.getAdditionalMessages();
        assertEquals(1, additional.size());
        assertEquals("arquivo@pousadamarealta.com.br", additional.get(0).to());
        assertEquals("[Cópia] Subject", additional.get(0).subject());
        assertEquals("Body", additional.get(0).body());
    }

    @Test
    void shouldNotCreateArchiveCopyForBlockedMessage() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        OutgoingMessage message = new OutgoingMessage("ana@gmail.com", "Subject", "Body");
        MessageContext context = new MessageContext(message);
        context.setStatus(MessageStatus.BLOCKED);

        rule.process(context);

        assertEquals(0, context.getAdditionalMessages().size());
    }

    @Test
    void shouldApplyAllRulesInSequence() {
        MessageDispatcher dispatcher = new MessageDispatcher(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        OutgoingMessage message = new OutgoingMessage("ana@gmail.com", "Subject", "Body");
        MessageContext context = dispatcher.dispatch(message);

        assertEquals(MessageStatus.SENT, context.getStatus());
        assertEquals(1, context.getAdditionalMessages().size());
    }

    @Test
    void shouldBlockAndNotCreateCopy() {
        MessageDispatcher dispatcher = new MessageDispatcher(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        OutgoingMessage message = new OutgoingMessage("ana.gmail.com", "Subject", "Body");
        MessageContext context = dispatcher.dispatch(message);

        assertEquals(MessageStatus.BLOCKED, context.getStatus());
        assertEquals(0, context.getAdditionalMessages().size());
    }
}
