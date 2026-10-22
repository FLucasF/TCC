package com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marealta.mail.MailPipeline;
import com.marealta.mail.MailSettings;
import com.marealta.mail.Message;
import com.marealta.mail.MessageStatus;
import com.marealta.mail.Outbox;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MailPipelineTest {
    private final List<Message> log = new ArrayList<>();
    private final Outbox sink = m -> {
        Message recorded = m.withId(log.size() + 1);
        log.add(recorded);
        return recorded;
    };

    private Outbox pipeline(boolean test, boolean archive) {
        return MailPipeline.build(sink, new MailSettings(test, archive));
    }

    @Test
    void liveSendsOriginalThenArchiveCopy() {
        pipeline(false, true).send(Message.email("ana@gmail.com", "Oi", "texto"));
        assertEquals(2, log.size());
        assertEquals("ana@gmail.com", log.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", log.get(1).to());
        assertEquals("[Cópia] Oi", log.get(1).subject());
        assertEquals("texto", log.get(1).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (String bad : new String[] {"ana@gmail", "ana.gmail.com", "a@@b.com", "@b.com", "a@.com", "a@b.", ""}) {
            log.clear();
            pipeline(false, true).send(Message.email(bad, "Oi", "texto"));
            assertEquals(1, log.size(), bad);
            assertEquals(MessageStatus.BLOCKED, log.get(0).status(), bad);
            assertEquals(bad, log.get(0).to());
        }
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        pipeline(true, true).send(Message.email("ana@gmail.com", "Oi", "texto"));
        assertEquals(1, log.size());
        Message m = log.get(0);
        assertEquals("testes@pousadamarealta.com.br", m.to());
        assertEquals("[TESTE] Oi", m.subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", m.body());
    }

    @Test
    void testModeStillBlocksInvalidKeepingOriginalAddress() {
        pipeline(true, true).send(Message.email("ana@gmail", "Oi", "texto"));
        assertEquals(1, log.size());
        assertEquals(MessageStatus.BLOCKED, log.get(0).status());
        assertEquals("ana@gmail", log.get(0).to());
    }

    @Test
    void installationWithoutArchiveSendsOnlyOriginal() {
        pipeline(false, false).send(Message.email("ana@gmail.com", "Oi", "texto"));
        assertEquals(1, log.size());
    }
}
