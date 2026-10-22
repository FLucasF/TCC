package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SendingRulesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private EmailSender sender(Map<String, String> environment) {
        return new EmailSender(sentMessages, SendingRules.from(environment));
    }

    @Test
    void validAddresses() {
        assertTrue(SendingRules.ValidAddressRule.isValid("ana@gmail.com"));
        assertTrue(SendingRules.ValidAddressRule.isValid("a@b.c.d"));
        for (String bad : new String[] {"ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com",
                "ana@.com", "ana@gmail.", "ana@.", "", "ana@"}) {
            assertFalse(SendingRules.ValidAddressRule.isValid(bad), bad);
        }
    }

    @Test
    void anInvalidAddressIsBlockedWithoutCopy() {
        Message result = sender(Map.of()).send("ana@gmail", "Assunto", "Texto");

        assertEquals(MessageStatus.BLOCKED, result.status());
        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void aSentMessageGetsAnArchiveCopyRightAfterIt() {
        sender(Map.of()).send("ana@gmail.com", "Assunto", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Assunto", all.get(1).subject());
        assertEquals("Texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void testModeRedirectsAndHasNoArchiveCopy() {
        sender(Map.of("HOTEL_TEST_MODE", "true")).send("ana@gmail.com", "Assunto", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Assunto", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        sender(Map.of("HOTEL_TEST_MODE", "true")).send("ana.gmail.com", "Assunto", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana.gmail.com", all.get(0).to());
        assertEquals("Assunto", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void anInstallationCanTurnTheArchiveCopyOff() {
        sender(Map.of("HOTEL_ARCHIVE_COPY", "false")).send("ana@gmail.com", "Assunto", "Texto");

        assertEquals(1, sentMessages.all().size());
    }
}
