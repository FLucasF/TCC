package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private EmailSender sender(boolean testMode) {
        return new EmailSender(sentMessages, MessageRules.forInstallation(testMode));
    }

    @Test
    void sendsTheMessageAndAnArchiveCopyRightAfterIt() {
        sender(false).send("ana@gmail.com", "Olá", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Olá", all.get(1).subject());
        assertEquals("Texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void blocksInvalidAddressesWithoutACopy() {
        for (String address : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com", "a@.com", "a@gmail.")) {
            assertFalse(ValidAddress.isValid(address), address);
        }
        assertTrue(ValidAddress.isValid("ana@gmail.com"));

        Message message = sender(false).send("ana@gmail", "Olá", "Texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals("ana@gmail", message.to());
        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void inTestModeSendsOnlyToTheTestAddress() {
        Message message = sender(true).send("ana@gmail.com", "Olá", "Texto");

        assertEquals("testes@pousadamarealta.com.br", message.to());
        assertEquals("[TESTE] Olá", message.subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", message.body());
        assertEquals(MessageStatus.SENT, message.status());
        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void inTestModeInvalidAddressesAreBlockedWithTheOriginalAddress() {
        Message message = sender(true).send("ana@gmail", "Olá", "Texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals("ana@gmail", message.to());
        assertEquals("Olá", message.subject());
        assertEquals(1, sentMessages.all().size());
    }
}
