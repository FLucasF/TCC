package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestModeRuleTest {

    @Test
    public void testTestModeRedirection() {
        TestModeRule rule = new TestModeRule();
        Message message = new Message(MessageChannel.EMAIL, "guest@example.com", "Subject", "Body", MessageStatus.SENT);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTo()).isEqualTo("testes@pousadamarealta.com.br");
        assertThat(result.get(0).getSubject()).isEqualTo("[TESTE] Subject");
        assertThat(result.get(0).getBody()).startsWith("Destinatário original: guest@example.com\nBody");
    }

    @Test
    public void testTestModeDoesNotAffectBlocked() {
        TestModeRule rule = new TestModeRule();
        Message message = new Message(MessageChannel.EMAIL, "invalid", "Subject", "Body", MessageStatus.BLOCKED);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTo()).isEqualTo("invalid");
        assertThat(result.get(0).getSubject()).isEqualTo("Subject");
    }
}
