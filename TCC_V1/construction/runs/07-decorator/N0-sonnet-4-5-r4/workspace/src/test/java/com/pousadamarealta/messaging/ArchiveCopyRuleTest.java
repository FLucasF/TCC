package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchiveCopyRuleTest {

    @Test
    public void testArchiveCopyCreated() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        Message message = new Message(MessageChannel.EMAIL, "test@example.com", "Subject", "Body", MessageStatus.SENT);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTo()).isEqualTo("test@example.com");
        assertThat(result.get(1).getTo()).isEqualTo("arquivo@pousadamarealta.com.br");
        assertThat(result.get(1).getSubject()).isEqualTo("[Cópia] Subject");
        assertThat(result.get(1).getBody()).isEqualTo("Body");
    }

    @Test
    public void testArchiveCopyNotCreatedForBlocked() {
        ArchiveCopyRule rule = new ArchiveCopyRule();
        Message message = new Message(MessageChannel.EMAIL, "invalid", "Subject", "Body", MessageStatus.BLOCKED);

        List<Message> result = rule.apply(message);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTo()).isEqualTo("invalid");
    }
}
