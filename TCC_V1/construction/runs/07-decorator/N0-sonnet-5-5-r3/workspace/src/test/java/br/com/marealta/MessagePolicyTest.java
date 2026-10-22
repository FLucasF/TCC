package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import br.com.marealta.mail.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagePolicyTest {
    static final Instant NOON = Instant.parse("2027-03-10T15:00:00Z"); // 12h em São Paulo
    static final Instant NIGHT = Instant.parse("2027-03-10T05:00:00Z"); // 02h em São Paulo

    static MailSettings live() { return MailSettings.defaults(false); }
    static MailSettings test() { return MailSettings.defaults(true); }

    static List<Outgoing> run(MailSettings s, String to, Instant now, int sentToday) {
        return new MessagePolicy(s).apply(to, "Assunto", "Corpo", now, a -> sentToday);
    }

    @Test
    void addressValidation() {
        assertTrue(MessagePolicy.isValidAddress("ana@gmail.com"));
        assertFalse(MessagePolicy.isValidAddress("ana@gmail"));
        assertFalse(MessagePolicy.isValidAddress("ana.gmail.com"));
        assertFalse(MessagePolicy.isValidAddress("a@b@c.com"));
        assertFalse(MessagePolicy.isValidAddress("@gmail.com"));
        assertFalse(MessagePolicy.isValidAddress("ana@.com"));
        assertFalse(MessagePolicy.isValidAddress("ana@gmail."));
    }

    @Test
    void liveSendsWithArchiveCopy() {
        var out = run(live(), "ana@gmail.com", NOON, 0);
        assertEquals(2, out.size());
        assertEquals("ana@gmail.com", out.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", out.get(1).to());
        assertEquals("[Cópia] Assunto", out.get(1).subject());
        assertEquals("Corpo", out.get(1).body());
    }

    @Test
    void invalidIsBlockedWithoutCopy() {
        var out = run(live(), "ana@gmail", NOON, 0);
        assertEquals(1, out.size());
        assertEquals("BLOCKED", out.get(0).status());
        assertEquals("ana@gmail", out.get(0).to());
    }

    @Test
    void testModeRedirects() {
        var out = run(test(), "ana@gmail.com", NOON, 0);
        assertEquals(1, out.size());
        assertEquals("testes@pousadamarealta.com.br", out.get(0).to());
        assertEquals("[TESTE] Assunto", out.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nCorpo", out.get(0).body());
        var blocked = run(test(), "ana.gmail.com", NOON, 0);
        assertEquals("BLOCKED", blocked.get(0).status());
        assertEquals("ana.gmail.com", blocked.get(0).to());
    }

    @Test
    void secondInstallationWithoutArchive() {
        var s = MailSettings.defaults(false);
        s = new MailSettings(false, s.addressValidation(), Scope.OFF, s.quietHours(), s.dailyLimit(), 22, 7, 5,
                s.archiveAddress(), s.testAddress(), s.zone());
        assertEquals(1, run(s, "ana@gmail.com", NOON, 0).size());
    }

    @Test
    void quietHoursAndLimitScopes() {
        var d = live();
        var s = new MailSettings(false, Scope.ALWAYS, Scope.LIVE_ONLY, Scope.LIVE_ONLY, Scope.LIVE_ONLY, 22, 7, 2,
                d.archiveAddress(), d.testAddress(), d.zone());
        assertEquals("BLOCKED", run(s, "ana@gmail.com", NIGHT, 0).get(0).status());
        assertEquals("SENT", run(s, "ana@gmail.com", NOON, 1).get(0).status());
        assertEquals("BLOCKED", run(s, "ana@gmail.com", NOON, 2).get(0).status());
        var t = new MailSettings(true, Scope.ALWAYS, Scope.LIVE_ONLY, Scope.LIVE_ONLY, Scope.LIVE_ONLY, 22, 7, 2,
                d.archiveAddress(), d.testAddress(), d.zone());
        assertEquals("SENT", run(t, "ana@gmail.com", NIGHT, 9).get(0).status());
    }

    @Test
    void envParsing() {
        var s = MailSettings.fromEnv(java.util.Map.of("HOTEL_TEST_MODE", "true", "HOTEL_MAIL_ARCHIVE_COPY", "always"));
        assertTrue(s.testMode());
        assertEquals(Scope.ALWAYS, s.archiveCopy());
        assertFalse(MailSettings.fromEnv(java.util.Map.of()).testMode());
    }
}
