package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void close() {
        database.close();
    }

    private EmailSender sender(Map<String, String> environment, String utcTime) {
        Map<String, String> env = new java.util.HashMap<>(environment);
        env.putIfAbsent("HOTEL_ZONE", "UTC");
        return new EmailSender(
                sentMessages,
                MailSettings.fromEnvironment(env),
                Clock.fixed(Instant.parse("2027-03-10T" + utcTime + "Z"), ZoneOffset.UTC));
    }

    @Test
    void validatesAddresses() {
        for (String ok : List.of("ana@gmail.com", "a@b.c.d", "x@y.z")) {
            assertTrue(EmailSender.isValidAddress(ok), ok);
        }
        for (String bad : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com", "a@.com",
                "a@gmail.", "")) {
            assertFalse(EmailSender.isValidAddress(bad), bad);
        }
        assertFalse(EmailSender.isValidAddress(null));
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        sender(Map.of(), "12:00:00").send("ana@gmail", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void sentMessageGetsAnArchiveCopyRightAfterIt() {
        sender(Map.of(), "12:00:00").send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
    }

    @Test
    void archiveCopyCanBeTurnedOff() {
        sender(Map.of("HOTEL_ARCHIVE_COPY", "false"), "12:00:00").send("ana@gmail.com", "Oi", "texto");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void testModeRedirectsAndDoesNotArchive() {
        EmailSender sender = sender(Map.of("HOTEL_TEST_MODE", "true"), "12:00:00");
        sender.send("ana@gmail.com", "Oi", "texto");
        sender.send("ana.gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals(MessageStatus.BLOCKED, all.get(1).status());
        assertEquals("ana.gmail.com", all.get(1).to());
    }

    @Test
    void quietHoursBlockAcrossMidnightAndRespectScope() {
        Map<String, String> quiet = Map.of("HOTEL_QUIET_HOURS", "22:00-07:00");
        sender(quiet, "23:30:00").send("ana@gmail.com", "Oi", "t");
        sender(quiet, "03:00:00").send("ana@gmail.com", "Oi", "t");
        sender(quiet, "12:00:00").send("ana@gmail.com", "Oi", "t");
        sender(Map.of("HOTEL_QUIET_HOURS", "22:00-07:00", "HOTEL_QUIET_HOURS_SCOPE", "TEST_ONLY"), "23:30:00")
                .send("bia@gmail.com", "Oi", "t");

        List<MessageStatus> statuses = sentMessages.all().stream().map(Message::status).toList();
        assertEquals(List.of(MessageStatus.BLOCKED, MessageStatus.BLOCKED, MessageStatus.SENT, MessageStatus.SENT,
                MessageStatus.SENT, MessageStatus.SENT), statuses);
    }

    @Test
    void dailyLimitCountsOnlyThatGuestsSentMessagesAndAlsoInTestMode() {
        Map<String, String> env = Map.of("HOTEL_DAILY_LIMIT", "2", "HOTEL_TEST_MODE", "true");
        EmailSender sender = sender(env, "12:00:00");
        sender.send("ana@gmail.com", "1", "t");
        sender.send("bia@gmail.com", "1", "t");
        sender.send("ana@gmail.com", "2", "t");
        Message third = sender.send("ana@gmail.com", "3", "t");
        Message other = sender.send("bia@gmail.com", "2", "t");

        assertEquals(MessageStatus.BLOCKED, third.status());
        assertEquals(MessageStatus.SENT, other.status());
        // next day the counter starts over
        Message nextDay = new EmailSender(sentMessages, MailSettings.fromEnvironment(
                Map.of("HOTEL_DAILY_LIMIT", "2", "HOTEL_ZONE", "UTC")),
                Clock.fixed(Instant.parse("2027-03-11T12:00:00Z"), ZoneOffset.UTC)).send("ana@gmail.com", "4", "t");
        assertEquals(MessageStatus.SENT, nextDay.status());
    }
}
