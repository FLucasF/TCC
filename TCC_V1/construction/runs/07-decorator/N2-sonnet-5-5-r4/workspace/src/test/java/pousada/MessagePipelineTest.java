package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pousada.messaging.EmailAddress;
import pousada.messaging.MareAltaRules;
import pousada.messaging.Message;
import pousada.messaging.MessagePipeline;
import pousada.messaging.MessageSink;
import pousada.messaging.MessageStatus;

class MessagePipelineTest {
    private final List<Message> store = new ArrayList<>();
    private final MessageSink memory = m -> {
        store.add(m);
        return m;
    };

    private MessageSink pipeline(boolean testMode) {
        return MessagePipeline.assemble(testMode, memory, MareAltaRules.rules(memory));
    }

    @Test
    void emailValidation() {
        assertTrue(EmailAddress.isValid("ana@gmail.com"));
        assertFalse(EmailAddress.isValid("ana@gmail"));
        assertFalse(EmailAddress.isValid("ana.gmail.com"));
        assertFalse(EmailAddress.isValid("@gmail.com"));
        assertFalse(EmailAddress.isValid("a@@gmail.com"));
        assertFalse(EmailAddress.isValid("a@b@gmail.com"));
        assertFalse(EmailAddress.isValid("ana@.com"));
        assertFalse(EmailAddress.isValid("ana@gmail."));
    }

    @Test
    void normalModeSendsAndArchives() {
        pipeline(false).send(Message.email("ana@gmail.com", "Oi", "texto"));
        assertEquals(2, store.size());
        assertEquals("ana@gmail.com", store.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", store.get(1).to());
        assertEquals("[Cópia] Oi", store.get(1).subject());
        assertEquals("texto", store.get(1).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        pipeline(false).send(Message.email("ana@gmail", "Oi", "texto"));
        assertEquals(1, store.size());
        assertEquals(MessageStatus.BLOCKED, store.get(0).status());
        assertEquals("ana@gmail", store.get(0).to());
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        pipeline(true).send(Message.email("ana@gmail.com", "Oi", "texto"));
        assertEquals(1, store.size());
        Message m = store.get(0);
        assertEquals("testes@pousadamarealta.com.br", m.to());
        assertEquals("[TESTE] Oi", m.subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", m.body());
        assertEquals(MessageStatus.SENT, m.status());
    }

    @Test
    void testModeKeepsBlockingInvalidWithOriginalAddress() {
        pipeline(true).send(Message.email("ana.gmail.com", "Oi", "texto"));
        assertEquals(1, store.size());
        assertEquals(MessageStatus.BLOCKED, store.get(0).status());
        assertEquals("ana.gmail.com", store.get(0).to());
        assertEquals("Oi", store.get(0).subject());
    }

    @Test
    void anotherInstallationCanOmitTheArchive() {
        var sink = MessagePipeline.assemble(false, memory, MareAltaRules.rules(memory).subList(1, 3));
        sink.send(Message.email("ana@gmail.com", "Oi", "texto"));
        assertEquals(1, store.size());
    }
}
