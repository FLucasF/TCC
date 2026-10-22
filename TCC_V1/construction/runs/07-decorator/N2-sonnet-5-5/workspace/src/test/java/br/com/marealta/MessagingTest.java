package br.com.marealta;

import br.com.marealta.messaging.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessagingTest {
    private static final class MemoryStore implements MessageStore {
        final List<Message> messages = new ArrayList<>();

        public Message save(Message m) {
            Message saved = new Message((long) messages.size() + 1, m.channel(), m.to(), m.subject(), m.body(), m.status());
            messages.add(saved);
            return saved;
        }

        public List<Message> all() { return messages; }
    }

    @Test
    void emailValidation() {
        assertTrue(EmailAddress.isValid("ana@gmail.com"));
        assertTrue(EmailAddress.isValid("a@b.c.d"));
        assertFalse(EmailAddress.isValid("ana@gmail"));
        assertFalse(EmailAddress.isValid("ana.gmail.com"));
        assertFalse(EmailAddress.isValid("@gmail.com"));
        assertFalse(EmailAddress.isValid("a@@gmail.com"));
        assertFalse(EmailAddress.isValid("a@b@gmail.com"));
        assertFalse(EmailAddress.isValid("ana@.com"));
        assertFalse(EmailAddress.isValid("ana@gmail."));
    }

    @Test
    void normalModeSendsThenArchives() {
        MemoryStore store = new MemoryStore();
        Installation.marealta(store, false).send(Message.email("ana@gmail.com", "Oi", "Texto"));
        assertEquals(2, store.messages.size());
        assertEquals("ana@gmail.com", store.messages.get(0).to());
        assertEquals(Message.Status.SENT, store.messages.get(0).status());
        Message copy = store.messages.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", copy.to());
        assertEquals("[Cópia] Oi", copy.subject());
        assertEquals("Texto", copy.body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        MemoryStore store = new MemoryStore();
        Installation.marealta(store, false).send(Message.email("ana@gmail", "Oi", "Texto"));
        assertEquals(1, store.messages.size());
        assertEquals(Message.Status.BLOCKED, store.messages.get(0).status());
        assertEquals("ana@gmail", store.messages.get(0).to());
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        MemoryStore store = new MemoryStore();
        Installation.marealta(store, true).send(Message.email("ana@gmail.com", "Oi", "Texto"));
        assertEquals(1, store.messages.size());
        Message m = store.messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", m.to());
        assertEquals("[TESTE] Oi", m.subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", m.body());
        assertEquals(Message.Status.SENT, m.status());
    }

    @Test
    void testModeStillBlocksInvalidAddressKeepingOriginal() {
        MemoryStore store = new MemoryStore();
        Installation.marealta(store, true).send(Message.email("ana.gmail.com", "Oi", "Texto"));
        assertEquals(1, store.messages.size());
        Message m = store.messages.get(0);
        assertEquals(Message.Status.BLOCKED, m.status());
        assertEquals("ana.gmail.com", m.to());
        assertEquals("Oi", m.subject());
    }

    @Test
    void anotherInstallationPicksItsOwnRules() {
        MemoryStore store = new MemoryStore();
        List<SendingRule> rules = List.of(new SendingRule(Scope.ALWAYS, next -> new ValidatingSender(store, next)));
        MessagePipeline.assemble(new OutboxSender(store), rules, false).send(Message.email("a@b.com", "Oi", "T"));
        assertEquals(1, store.messages.size());
    }
}
