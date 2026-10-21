package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageRuleContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidationRuleTest {

    private final EmailValidationRule rule = new EmailValidationRule();

    @Test
    void shouldAllowValidEmail() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "ana@gmail.com", "Subject", "Body");

        rule.apply(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.SENT);
    }

    @Test
    void shouldBlockEmailWithoutAt() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "ana.gmail.com", "Subject", "Body");

        rule.apply(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void shouldBlockEmailWithoutDomain() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "ana@gmail", "Subject", "Body");

        rule.apply(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void shouldBlockEmailWithMultipleAts() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "ana@@gmail.com", "Subject", "Body");

        rule.apply(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }

    @Test
    void shouldBlockEmailStartingWithAt() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "@gmail.com", "Subject", "Body");

        rule.apply(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.BLOCKED);
    }
}
