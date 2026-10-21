package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageRuleContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestModeRedirectRuleTest {

    private final TestModeRedirectRule rule = new TestModeRedirectRule();

    @Test
    void shouldRedirectToTestEmail() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "hospede@example.com", "Reserva confirmada", "Corpo da mensagem");

        rule.apply(context);

        assertThat(context.getTo()).isEqualTo("testes@pousadamarealta.com.br");
        assertThat(context.getSubject()).isEqualTo("[TESTE] Reserva confirmada");
        assertThat(context.getBody()).startsWith("Destinatário original: hospede@example.com\n");
        assertThat(context.getBody()).contains("Corpo da mensagem");
    }

    @Test
    void shouldNotRedirectBlockedMessage() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "hospede@example.com", "Subject", "Body");
        context.setStatus(MessageStatus.BLOCKED);

        String originalTo = context.getTo();
        String originalSubject = context.getSubject();
        String originalBody = context.getBody();

        rule.apply(context);

        assertThat(context.getTo()).isEqualTo(originalTo);
        assertThat(context.getSubject()).isEqualTo(originalSubject);
        assertThat(context.getBody()).isEqualTo(originalBody);
    }
}
