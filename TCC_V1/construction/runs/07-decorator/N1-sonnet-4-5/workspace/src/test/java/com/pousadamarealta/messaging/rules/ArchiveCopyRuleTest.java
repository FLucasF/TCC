package com.pousadamarealta.messaging.rules;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.MessageRuleContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveCopyRuleTest {

    private final ArchiveCopyRule rule = new ArchiveCopyRule();

    @Test
    void shouldCreateArchiveCopyForSentMessage() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "hospede@example.com", "Reserva confirmada", "Corpo");

        rule.apply(context);

        assertThat(context.getAdditionalMessages()).hasSize(1);
        assertThat(context.getAdditionalMessages().get(0).getTo())
                .isEqualTo("arquivo@pousadamarealta.com.br");
        assertThat(context.getAdditionalMessages().get(0).getSubject())
                .isEqualTo("[Cópia] Reserva confirmada");
        assertThat(context.getAdditionalMessages().get(0).getBody())
                .isEqualTo("Corpo");
    }

    @Test
    void shouldNotCreateArchiveCopyForBlockedMessage() {
        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL, "hospede@example.com", "Subject", "Body");
        context.setStatus(MessageStatus.BLOCKED);

        rule.apply(context);

        assertThat(context.getAdditionalMessages()).isEmpty();
    }
}
