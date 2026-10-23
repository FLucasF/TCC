package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailValidationRuleTest {

    @Test
    public void testValidEmail() {
        EmailValidationRule rule = new EmailValidationRule();
        Message message = new Message(MessageChannel.EMAIL, "ana@gmail.com", "Test", "Body", null);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    public void testInvalidEmailMissingAt() {
        EmailValidationRule rule = new EmailValidationRule();
        Message message = new Message(MessageChannel.EMAIL, "ana.gmail.com", "Test", "Body", null);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    public void testInvalidEmailNoDomain() {
        EmailValidationRule rule = new EmailValidationRule();
        Message message = new Message(MessageChannel.EMAIL, "ana@gmail", "Test", "Body", null);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    public void testInvalidEmailMultipleAt() {
        EmailValidationRule rule = new EmailValidationRule();
        Message message = new Message(MessageChannel.EMAIL, "ana@@gmail.com", "Test", "Body", null);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    public void testInvalidEmailEmptyBeforeAt() {
        EmailValidationRule rule = new EmailValidationRule();
        Message message = new Message(MessageChannel.EMAIL, "@gmail.com", "Test", "Body", null);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }
}
