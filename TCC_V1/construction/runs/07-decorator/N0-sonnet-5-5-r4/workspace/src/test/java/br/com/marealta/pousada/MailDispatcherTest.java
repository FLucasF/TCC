package br.com.marealta.pousada;

import br.com.marealta.pousada.MailConfig.Scope;
import br.com.marealta.pousada.MailDispatcher.NewMessage;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailDispatcherTest {
    static class MemStore implements MailDispatcher.MailStore {
        final List<NewMessage> saved = new ArrayList<>();
        public void save(NewMessage m) { saved.add(m); }
        public int countSent(String a, Instant f, Instant t) {
            return (int) saved.stream().filter(m -> m.status().equals("SENT") && !m.copy()
                    && m.originalTo().equalsIgnoreCase(a) && !m.createdAt().isBefore(f) && m.createdAt().isBefore(t)).count();
        }
    }

    static Clock at(int hour) {
        return Clock.fixed(ZonedDateTime.of(2027, 3, 10, hour, 0, 0, 0, MailDispatcher.ZONE).toInstant(), ZoneOffset.UTC);
    }

    static MailConfig cfg(boolean test) { return MailConfig.defaults(test); }

    @Test
    void validAddresses() {
        assertTrue(MailDispatcher.validAddress("ana@gmail.com"));
        assertFalse(MailDispatcher.validAddress("ana@gmail"));
        assertFalse(MailDispatcher.validAddress("ana.gmail.com"));
        assertFalse(MailDispatcher.validAddress("@gmail.com"));
        assertFalse(MailDispatcher.validAddress("a@@gmail.com"));
        assertFalse(MailDispatcher.validAddress("a@b@gmail.com"));
        assertFalse(MailDispatcher.validAddress("ana@.com"));
        assertFalse(MailDispatcher.validAddress("ana@gmail."));
        assertFalse(MailDispatcher.validAddress(null));
    }

    @Test
    void liveSendsWithArchiveCopy() {
        var s = new MemStore();
        new MailDispatcher(cfg(false), at(12)).send(s, "ana@gmail.com", "Assunto", "Texto");
        assertEquals(2, s.saved.size());
        assertEquals("ana@gmail.com", s.saved.get(0).to());
        assertEquals("Assunto", s.saved.get(0).subject());
        assertEquals(MailConfig.ARCHIVE_ADDRESS, s.saved.get(1).to());
        assertEquals("[Cópia] Assunto", s.saved.get(1).subject());
        assertEquals("Texto", s.saved.get(1).body());
    }

    @Test
    void invalidIsBlockedWithoutCopy() {
        for (boolean test : new boolean[]{false, true}) {
            var s = new MemStore();
            new MailDispatcher(cfg(test), at(12)).send(s, "ana@gmail", "A", "T");
            assertEquals(1, s.saved.size());
            assertEquals("BLOCKED", s.saved.get(0).status());
            assertEquals("ana@gmail", s.saved.get(0).to());
            assertEquals("A", s.saved.get(0).subject());
        }
    }

    @Test
    void testModeRedirectsAndSkipsArchive() {
        var s = new MemStore();
        new MailDispatcher(cfg(true), at(12)).send(s, "ana@gmail.com", "Assunto", "Texto");
        assertEquals(1, s.saved.size());
        var m = s.saved.get(0);
        assertEquals(MailConfig.TEST_ADDRESS, m.to());
        assertEquals("[TESTE] Assunto", m.subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", m.body());
    }

    @Test
    void archiveInTestModeIsAlsoRedirected() {
        var c = new MailConfig(true, Scope.ALWAYS, Scope.OFF, 0, 6, Scope.OFF, 5);
        var s = new MemStore();
        new MailDispatcher(c, at(12)).send(s, "ana@gmail.com", "A", "T");
        assertEquals(2, s.saved.size());
        assertTrue(s.saved.stream().allMatch(m -> m.to().equals(MailConfig.TEST_ADDRESS)));
    }

    @Test
    void archiveCanBeOff() {
        var c = new MailConfig(false, Scope.OFF, Scope.OFF, 0, 6, Scope.OFF, 5);
        var s = new MemStore();
        new MailDispatcher(c, at(12)).send(s, "ana@gmail.com", "A", "T");
        assertEquals(1, s.saved.size());
    }

    @Test
    void quietHoursScopes() {
        var always = new MailConfig(false, Scope.OFF, Scope.ALWAYS, 0, 6, Scope.OFF, 5);
        var testOnly = new MailConfig(false, Scope.OFF, Scope.TEST_ONLY, 0, 6, Scope.OFF, 5);
        var s = new MemStore();
        new MailDispatcher(always, at(3)).send(s, "ana@gmail.com", "A", "T");
        assertEquals("BLOCKED", s.saved.get(0).status());
        new MailDispatcher(testOnly, at(3)).send(s, "ana@gmail.com", "A", "T");
        assertEquals("SENT", s.saved.get(1).status());
        new MailDispatcher(always, at(12)).send(s, "ana@gmail.com", "A", "T");
        assertEquals("SENT", s.saved.get(2).status());
    }

    @Test
    void dailyLimit() {
        var c = new MailConfig(false, Scope.LIVE_ONLY, Scope.OFF, 0, 6, Scope.ALWAYS, 2);
        var d = new MailDispatcher(c, at(12));
        var s = new MemStore();
        for (int i = 0; i < 3; i++) d.send(s, "ana@gmail.com", "A", "T");
        d.send(s, "bia@gmail.com", "A", "T");
        List<String> st = s.saved.stream().filter(m -> !m.copy()).map(NewMessage::status).toList();
        assertEquals(List.of("SENT", "SENT", "BLOCKED", "SENT"), st);
    }

    @Test
    void configFromEnv() {
        var c = MailConfig.fromEnv(Map.of("HOTEL_TEST_MODE", "true", "HOTEL_MAIL_ARCHIVE_COPY", "off"));
        assertTrue(c.testMode());
        assertEquals(Scope.OFF, c.archiveCopy());
        assertFalse(MailConfig.fromEnv(Map.of()).testMode());
    }
}
