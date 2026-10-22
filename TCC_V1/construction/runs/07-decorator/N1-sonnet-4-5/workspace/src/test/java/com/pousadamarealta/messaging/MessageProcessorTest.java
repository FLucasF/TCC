package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageProcessorTest {

    @Test
    void shouldSendValidEmail() {
        MessageProcessor processor = new MessageProcessor(List.of(new EmailValidationRule()));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "user@example.com",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(1);
        assertThat(processed.get(0).getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(processed.get(0).getTo()).isEqualTo("user@example.com");
    }

    @Test
    void shouldBlockInvalidEmailWithoutAt() {
        MessageProcessor processor = new MessageProcessor(List.of(new EmailValidationRule()));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "usergmail.com",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(1);
        assertThat(processed.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void shouldBlockInvalidEmailWithoutDot() {
        MessageProcessor processor = new MessageProcessor(List.of(new EmailValidationRule()));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "user@gmailcom",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(1);
        assertThat(processed.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void shouldBlockInvalidEmailWithMultipleAts() {
        MessageProcessor processor = new MessageProcessor(List.of(new EmailValidationRule()));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "user@@gmail.com",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(1);
        assertThat(processed.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void shouldRedirectToTestModeEmail() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new TestModeRule()
        ));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "user@example.com",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(1);
        ProcessedMessage message = processed.get(0);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(message.getTo()).isEqualTo("testes@pousadamarealta.com.br");
        assertThat(message.getSubject()).isEqualTo("[TESTE] Test Subject");
        assertThat(message.getBody()).startsWith("Destinatário original: user@example.com\n");
        assertThat(message.getBody()).contains("Test Body");
    }

    @Test
    void shouldNotRedirectBlockedMessageInTestMode() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new TestModeRule()
        ));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "invalidgmail.com",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(1);
        ProcessedMessage message = processed.get(0);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.BLOCKED);
        assertThat(message.getTo()).isEqualTo("invalidgmail.com");
    }

    @Test
    void shouldCreateArchiveCopy() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new ArchiveCopyRule()
        ));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "user@example.com",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(2);

        ProcessedMessage original = processed.get(0);
        assertThat(original.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(original.getTo()).isEqualTo("user@example.com");
        assertThat(original.getSubject()).isEqualTo("Test Subject");

        ProcessedMessage copy = processed.get(1);
        assertThat(copy.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(copy.getTo()).isEqualTo("arquivo@pousadamarealta.com.br");
        assertThat(copy.getSubject()).isEqualTo("[Cópia] Test Subject");
        assertThat(copy.getBody()).isEqualTo("Test Body");
    }

    @Test
    void shouldNotCreateArchiveCopyForBlockedMessage() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new ArchiveCopyRule()
        ));

        PendingMessage pending = new PendingMessage(
                MessageChannel.EMAIL,
                "invalidgmail.com",
                "Test Subject",
                "Test Body"
        );

        List<ProcessedMessage> processed = processor.process(pending);

        assertThat(processed).hasSize(1);
        assertThat(processed.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }
}
