package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import com.pousadamarealta.messaging.rules.ArchiveCopyRule;
import com.pousadamarealta.messaging.rules.EmailValidationRule;
import com.pousadamarealta.messaging.rules.TestModeRedirectRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageProcessorIntegrationTest {

    @Test
    void shouldApplyValidationAndArchiveCopyRulesInProduction() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new ArchiveCopyRule()
        ));

        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL,
                "hospede@example.com",
                "Reserva confirmada",
                "Corpo da mensagem"
        );

        processor.process(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(context.getTo()).isEqualTo("hospede@example.com");
        assertThat(context.getSubject()).isEqualTo("Reserva confirmada");

        assertThat(context.getAdditionalMessages()).hasSize(1);
        assertThat(context.getAdditionalMessages().get(0).getTo())
                .isEqualTo("arquivo@pousadamarealta.com.br");
        assertThat(context.getAdditionalMessages().get(0).getSubject())
                .isEqualTo("[Cópia] Reserva confirmada");
    }

    @Test
    void shouldBlockInvalidEmailAndNotCreateArchiveCopy() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new ArchiveCopyRule()
        ));

        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL,
                "hospede.invalid",
                "Reserva confirmada",
                "Corpo da mensagem"
        );

        processor.process(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.BLOCKED);
        assertThat(context.getAdditionalMessages()).isEmpty();
    }

    @Test
    void shouldRedirectToTestEmailInTestMode() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new TestModeRedirectRule()
        ));

        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL,
                "hospede@example.com",
                "Reserva confirmada",
                "Corpo da mensagem"
        );

        processor.process(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(context.getTo()).isEqualTo("testes@pousadamarealta.com.br");
        assertThat(context.getSubject()).isEqualTo("[TESTE] Reserva confirmada");
        assertThat(context.getBody()).startsWith("Destinatário original: hospede@example.com");
    }

    @Test
    void shouldBlockInvalidEmailEvenInTestMode() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationRule(),
                new TestModeRedirectRule()
        ));

        MessageRuleContext context = new MessageRuleContext(
                MessageChannel.EMAIL,
                "hospede.invalid",
                "Reserva confirmada",
                "Corpo da mensagem"
        );

        processor.process(context);

        assertThat(context.getStatus()).isEqualTo(MessageStatus.BLOCKED);
        assertThat(context.getTo()).isEqualTo("hospede.invalid");
    }
}
