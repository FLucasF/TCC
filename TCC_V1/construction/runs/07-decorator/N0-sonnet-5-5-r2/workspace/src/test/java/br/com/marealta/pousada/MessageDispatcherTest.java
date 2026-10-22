package br.com.marealta.pousada;

import br.com.marealta.pousada.Config.QuietHours;
import br.com.marealta.pousada.Config.Scope;
import br.com.marealta.pousada.Domain.Message;
import br.com.marealta.pousada.Domain.MessageStatus;
import br.com.marealta.pousada.MessageDispatcher.Draft;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageDispatcherTest {
    static class MemStore implements MessageDispatcher.Store {
        final List<Message> all = new ArrayList<>();
        final List<String> keys = new ArrayList<>();

        public Message save(Draft d, MessageStatus s, String key) {
            Message m = new Message(all.size() + 1, d.channel(), d.to(), d.subject(), d.body(), s);
            all.add(m);
            keys.add(key);
            return m;
        }

        public int countSent(String key, Instant from, Instant to) {
            int n = 0;
            for (int i = 0; i < all.size(); i++) {
                if (key.equals(keys.get(i)) && all.get(i).status() == MessageStatus.SENT) n++;
            }
            return n;
        }
    }

    private static final ZoneId UTC = ZoneOffset.UTC;
    private static Clock at(String instant) { return Clock.fixed(Instant.parse(instant), UTC); }
    private static final Draft MSG = new Draft("EMAIL", "ana@gmail.com", "Assunto", "Corpo");

    private static Config cfg(boolean test, Scope archive, QuietHours qh, Scope qs, Integer limit, Scope ls) {
        return new Config(test, archive, qh, qs, limit, ls, UTC);
    }

    @Test
    void validAddresses() {
        assertTrue(MessageDispatcher.isValidAddress("ana@gmail.com"));
        assertTrue(MessageDispatcher.isValidAddress("a@b.c.d"));
        for (String bad : new String[]{"ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com", "a@.com",
                "a@com.", "a@b .com", "", "a@"}) {
            assertFalse(MessageDispatcher.isValidAddress(bad), bad);
        }
    }

    @Test
    void normalModeSendsAndArchives() {
        var store = new MemStore();
        var out = new MessageDispatcher(Config.defaults(false), Clock.systemUTC()).dispatch(store, MSG);
        assertEquals(2, out.size());
        assertEquals("ana@gmail.com", out.get(0).to());
        assertEquals("Assunto", out.get(0).subject());
        assertEquals(MessageDispatcher.ARCHIVE_ADDRESS, out.get(1).to());
        assertEquals("[Cópia] Assunto", out.get(1).subject());
        assertEquals("Corpo", out.get(1).body());
        assertEquals(MessageStatus.SENT, out.get(1).status());
    }

    @Test
    void invalidAddressBlockedWithoutCopy() {
        for (boolean test : new boolean[]{false, true}) {
            var store = new MemStore();
            var out = new MessageDispatcher(Config.defaults(test), Clock.systemUTC())
                    .dispatch(store, new Draft("EMAIL", "ana@gmail", "S", "B"));
            assertEquals(1, out.size());
            assertEquals(MessageStatus.BLOCKED, out.get(0).status());
            assertEquals("ana@gmail", out.get(0).to());
            assertEquals("S", out.get(0).subject());
            assertEquals("B", out.get(0).body());
        }
    }

    @Test
    void testModeRedirectsAndSkipsArchive() {
        var out = new MessageDispatcher(Config.defaults(true), Clock.systemUTC()).dispatch(new MemStore(), MSG);
        assertEquals(1, out.size());
        assertEquals(MessageDispatcher.TEST_ADDRESS, out.get(0).to());
        assertEquals("[TESTE] Assunto", out.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nCorpo", out.get(0).body());
    }

    @Test
    void archiveCopyInTestModeIsStillRedirected() {
        var cfg = cfg(true, Scope.ALWAYS, null, Scope.ALWAYS, null, Scope.ALWAYS);
        var out = new MessageDispatcher(cfg, Clock.systemUTC()).dispatch(new MemStore(), MSG);
        assertEquals(2, out.size());
        assertTrue(out.stream().allMatch(m -> m.to().equals(MessageDispatcher.TEST_ADDRESS)));
    }

    @Test
    void installationWithoutArchive() {
        var cfg = cfg(false, Scope.NEVER, null, Scope.ALWAYS, null, Scope.ALWAYS);
        assertEquals(1, new MessageDispatcher(cfg, Clock.systemUTC()).dispatch(new MemStore(), MSG).size());
    }

    @Test
    void quietHoursWrapAroundMidnightAndScope() {
        var qh = new QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0));
        var always = cfg(false, Scope.NON_TEST_ONLY, qh, Scope.ALWAYS, null, Scope.ALWAYS);
        assertEquals(MessageStatus.BLOCKED,
                new MessageDispatcher(always, at("2027-03-10T03:00:00Z")).dispatch(new MemStore(), MSG).get(0).status());
        assertEquals(MessageStatus.SENT,
                new MessageDispatcher(always, at("2027-03-10T12:00:00Z")).dispatch(new MemStore(), MSG).get(0).status());
        var testOnly = cfg(false, Scope.NON_TEST_ONLY, qh, Scope.TEST_ONLY, null, Scope.ALWAYS);
        assertEquals(MessageStatus.SENT,
                new MessageDispatcher(testOnly, at("2027-03-10T03:00:00Z")).dispatch(new MemStore(), MSG).get(0).status());
    }

    @Test
    void dailyLimitPerGuestCountsOnlyOriginals() {
        var cfg = cfg(false, Scope.NON_TEST_ONLY, null, Scope.ALWAYS, 2, Scope.ALWAYS);
        var d = new MessageDispatcher(cfg, Clock.systemUTC());
        var store = new MemStore();
        assertEquals(MessageStatus.SENT, d.dispatch(store, MSG).get(0).status());
        assertEquals(MessageStatus.SENT, d.dispatch(store, MSG).get(0).status());
        assertEquals(MessageStatus.BLOCKED, d.dispatch(store, MSG).get(0).status());
        assertEquals(MessageStatus.SENT,
                d.dispatch(store, new Draft("EMAIL", "bia@gmail.com", "S", "B")).get(0).status());
    }
}
