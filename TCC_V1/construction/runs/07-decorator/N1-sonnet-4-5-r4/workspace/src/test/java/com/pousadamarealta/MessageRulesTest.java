package com.pousadamarealta;

import com.pousadamarealta.messaging.*;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageChannel;
import com.pousadamarealta.model.MessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageRulesTest {

    @Test
    void testEmailValidationRuleBlocksInvalidEmail() {
        Message message = new Message(MessageChannel.EMAIL, "invalid-email", "Test", "Body");
        MessageRulePipeline pipeline = new MessageRulePipeline(List.of(new EmailValidationRule()));

        List<Message> result = pipeline.process(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void testEmailValidationRuleAllowsValidEmail() {
        Message message = new Message(MessageChannel.EMAIL, "user@example.com", "Test", "Body");
        MessageRulePipeline pipeline = new MessageRulePipeline(List.of(new EmailValidationRule()));

        List<Message> result = pipeline.process(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    void testTestModeRuleRedirectsMessage() {
        Message message = new Message(MessageChannel.EMAIL, "user@example.com", "Test Subject", "Body");
        MessageRulePipeline pipeline = new MessageRulePipeline(List.of(
            new EmailValidationRule(),
            new TestModeRule()
        ));

        List<Message> result = pipeline.process(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTo()).isEqualTo("testes@pousadamarealta.com.br");
        assertThat(result.get(0).getSubject()).startsWith("[TESTE]");
        assertThat(result.get(0).getBody()).startsWith("Destinatário original: user@example.com");
    }

    @Test
    void testArchiveCopyRuleCreatesArchiveCopy() {
        Message message = new Message(MessageChannel.EMAIL, "user@example.com", "Test Subject", "Body");
        MessageRulePipeline pipeline = new MessageRulePipeline(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        List<Message> result = pipeline.process(message);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTo()).isEqualTo("user@example.com");
        assertThat(result.get(1).getTo()).isEqualTo("arquivo@pousadamarealta.com.br");
        assertThat(result.get(1).getSubject()).startsWith("[Cópia]");
    }

    @Test
    void testBlockedMessageDoesNotCreateArchiveCopy() {
        Message message = new Message(MessageChannel.EMAIL, "invalid", "Test Subject", "Body");
        MessageRulePipeline pipeline = new MessageRulePipeline(List.of(
            new EmailValidationRule(),
            new ArchiveCopyRule()
        ));

        List<Message> result = pipeline.process(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void testProductionModePipeline() {
        MessageRulePipeline pipeline = MessageRulePipelineFactory.create(false);
        Message message = new Message(MessageChannel.EMAIL, "user@example.com", "Subject", "Body");

        List<Message> result = pipeline.process(message);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTo()).isEqualTo("user@example.com");
        assertThat(result.get(1).getTo()).isEqualTo("arquivo@pousadamarealta.com.br");
    }

    @Test
    void testTestModePipeline() {
        MessageRulePipeline pipeline = MessageRulePipelineFactory.create(true);
        Message message = new Message(MessageChannel.EMAIL, "user@example.com", "Subject", "Body");

        List<Message> result = pipeline.process(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTo()).isEqualTo("testes@pousadamarealta.com.br");
        assertThat(result.get(0).getSubject()).startsWith("[TESTE]");
    }
}
