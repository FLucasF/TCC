package br.com.marealta;

import br.com.marealta.messaging.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageRulesTest {
    static class MemoryLog implements MessageLog {
        final List<StoredMessage> items = new ArrayList<>();
        public void save(Message m, MessageStatus s) {
            items.add(new StoredMessage(items.size() + 1, m.channel(), m.to(), m.subject(), m.body(), s));
        }
        public List<StoredMessage> all() { return items; }
    }

    private MemoryLog send(boolean test, boolean archive, String to) {
        MemoryLog log = new MemoryLog();
        Main.messageSender(log, test, archive).send(Message.email(to, "Assunto", "Texto"));
        return log;
    }

    @Test
    void normalModeSendsAndArchives() {
        var items = send(false, true, "ana@gmail.com").items;
        assertEquals(2, items.size());
        assertEquals("ana@gmail.com", items.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", items.get(1).to());
        assertEquals("[Cópia] Assunto", items.get(1).subject());
        assertEquals("Texto", items.get(1).body());
    }

    @Test
    void installationWithoutArchive() {
        assertEquals(1, send(false, false, "ana@gmail.com").items.size());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        for (boolean test : new boolean[]{false, true}) {
            for (String bad : List.of("ana@gmail", "ana.gmail.com", "a@@b.com", "@b.com", "a@.com", "a@b.")) {
                var items = send(test, true, bad).items;
                assertEquals(1, items.size(), bad);
                assertEquals(MessageStatus.BLOCKED, items.get(0).status());
                assertEquals(bad, items.get(0).to());
                assertEquals("Assunto", items.get(0).subject());
            }
        }
    }

    @Test
    void testModeRedirects() {
        var items = send(true, true, "ana@gmail.com").items;
        assertEquals(1, items.size());
        var m = items.get(0);
        assertEquals("testes@pousadamarealta.com.br", m.to());
        assertEquals("[TESTE] Assunto", m.subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", m.body());
        assertEquals(MessageStatus.SENT, m.status());
    }
}
