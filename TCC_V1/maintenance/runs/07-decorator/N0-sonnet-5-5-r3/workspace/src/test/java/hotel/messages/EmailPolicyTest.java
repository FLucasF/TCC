package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailPolicyTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private EmailSender sender(boolean testMode, String rules) {
        return new EmailSender(sentMessages, EmailPolicy.forRules(testMode, rules));
    }

    @Test
    void validatesAddresses() {
        assertTrue(EmailRules.isValidAddress("ana@gmail.com"));
        assertTrue(EmailRules.isValidAddress("a@b.c.d"));
        for (String bad : new String[] {"ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com",
                "ana@.com", "ana@gmail.", "", null}) {
            assertFalse(EmailRules.isValidAddress(bad), String.valueOf(bad));
        }
    }

    @Test
    void invalidAddressIsBlockedAndNotCopied() {
        sender(false, null).send("ana@gmail", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void sentMessageIsFollowedByAnArchiveCopy() {
        sender(false, null).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        sender(true, null).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
    }

    @Test
    void testModeStillBlocksInvalidAddressesWithTheOriginalAddress() {
        sender(true, null).send("ana@gmail", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
    }

    @Test
    void anInstallationCanLeaveTheArchiveCopyOut() {
        sender(false, "address-validation").send("ana@gmail.com", "Oi", "texto");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void testModeRedirectCannotBeConfiguredAway() {
        sender(true, "archive-copy:always").send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[Cópia] [TESTE] Oi", all.get(1).subject());
    }
}
