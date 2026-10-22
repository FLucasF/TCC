package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private static MessagingConfig utc() {
        return MessagingConfig.fromEnvironment(Map.of("HOTEL_TIMEZONE", "UTC"));
    }

    private EmailSender sender(MessagingConfig config, String instant) {
        return new EmailSender(sentMessages, config, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private EmailSender sender(MessagingConfig config) {
        return sender(config, "2027-03-10T15:00:00Z");
    }

    @Test
    void validAddresses() {
        assertTrue(EmailSender.isValidAddress("ana@gmail.com"));
        assertTrue(EmailSender.isValidAddress("a@b.c.d"));
        for (String bad : new String[] {"ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com",
                "a@.com", "a@com.", "", null}) {
            assertFalse(EmailSender.isValidAddress(bad), String.valueOf(bad));
        }
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        Message message = sender(utc()).send("ana@gmail", "Oi", "texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void sentMessageGetsArchiveCopyRightAfter() {
        sender(utc()).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void installationCanTurnTheArchiveCopyOff() {
        sender(utc().withArchiveCopy(RuleScope.OFF)).send("ana@gmail.com", "Oi", "t");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void testModeRedirectsAndHasNoCopy() {
        sender(utc().withTestMode(true)).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
    }

    @Test
    void testModeStillBlocksInvalidAddressKeepingTheOriginal() {
        sender(utc().withTestMode(true)).send("ana.gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana.gmail.com", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
    }

    @Test
    void quietHoursBlockAtNightOnly() {
        MessagingConfig config = utc().withQuietHours(RuleScope.ALWAYS, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertEquals(MessageStatus.BLOCKED, sender(config, "2027-03-10T02:00:00Z").send("a@b.com", "s", "b").status());
        assertEquals(MessageStatus.BLOCKED, sender(config, "2027-03-10T23:00:00Z").send("a@b.com", "s", "b").status());
        assertEquals(MessageStatus.SENT, sender(config, "2027-03-10T07:00:00Z").send("a@b.com", "s", "b").status());
    }

    @Test
    void ruleScopeDecidesTheMode() {
        MessagingConfig config = utc().withQuietHours(RuleScope.TEST_MODE_ONLY, LocalTime.of(22, 0), LocalTime.of(7, 0));

        assertEquals(MessageStatus.SENT, sender(config, "2027-03-10T02:00:00Z").send("a@b.com", "s", "b").status());
        assertEquals(MessageStatus.BLOCKED,
                sender(config.withTestMode(true), "2027-03-10T02:00:00Z").send("a@b.com", "s", "b").status());
    }

    @Test
    void dailyLimitPerGuestCountsOnlyRealMessages() {
        MessagingConfig config = utc().withDailyLimit(RuleScope.ALWAYS, 2);
        EmailSender sender = sender(config);

        assertEquals(MessageStatus.SENT, sender.send("a@b.com", "1", "b").status());
        assertEquals(MessageStatus.SENT, sender.send("a@b.com", "2", "b").status());
        assertEquals(MessageStatus.BLOCKED, sender.send("a@b.com", "3", "b").status());
        assertEquals(MessageStatus.SENT, sender.send("c@d.com", "1", "b").status());
        assertEquals(MessageStatus.SENT, sender(config, "2027-03-11T10:00:00Z").send("a@b.com", "4", "b").status());
    }
}
