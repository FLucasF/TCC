package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final SentMessages sent = new SentMessages();

    private static Clock at(String time) {
        return Clock.fixed(Instant.parse("2027-03-10T" + time + "Z"), ZoneOffset.UTC);
    }

    private EmailSender sender(String spec, boolean test, Clock clock) {
        return new EmailSender(sent, SendRules.parse(spec, test, clock));
    }

    @Test
    void addressValidation() {
        assertTrue(SendRules.isValidAddress("ana@gmail.com"));
        for (String bad : new String[] {"ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com", "a@.com", "a@com.", null}) {
            assertFalse(SendRules.isValidAddress(bad), String.valueOf(bad));
        }
    }

    @Test
    void invalidAddressIsBlockedAndNotArchived() {
        sender(SendRules.DEFAULT_SPEC, false, at("12:00:00")).send("ana@gmail", "Oi", "texto");
        List<Message> all = sent.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void sentMessageIsFollowedByArchiveCopy() {
        sender(SendRules.DEFAULT_SPEC, false, at("12:00:00")).send("ana@gmail.com", "Oi", "texto");
        List<Message> all = sent.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void testModeRedirectsWithoutArchive() {
        sender(SendRules.DEFAULT_SPEC, true, at("12:00:00")).send("ana@gmail.com", "Oi", "texto");
        sender(SendRules.DEFAULT_SPEC, true, at("12:00:00")).send("ana@gmail", "Oi", "texto");
        List<Message> all = sent.all();
        assertEquals(2, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
        assertEquals(MessageStatus.BLOCKED, all.get(1).status());
        assertEquals("ana@gmail", all.get(1).to());
    }

    @Test
    void quietHoursAndDailyLimitRespectScope() {
        String spec = "valid-address,quiet-hours:normal,daily-limit:test";
        sender(spec, false, at("03:00:00")).send("ana@gmail.com", "Oi", "t");
        assertEquals(MessageStatus.BLOCKED, sent.all().get(0).status());
        sender(spec, true, at("03:00:00")).send("ana@gmail.com", "Oi", "t");
        assertEquals(MessageStatus.SENT, sent.all().get(1).status());

        EmailSender limited = sender("daily-limit", false, at("12:00:00"));
        for (int i = 0; i < 6; i++) {
            limited.send("bia@gmail.com", "Oi", "t");
        }
        assertEquals(MessageStatus.BLOCKED, sent.all().get(7).status());
        assertEquals(MessageStatus.SENT, sent.all().get(6).status());
    }
}
