package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageSendersTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void sendsAndKeepsACopyForTheArchive() {
        Message sent = MessageSenders.create(sentMessages, false, true).send("ana@gmail.com", "Oi", "Texto");

        assertEquals(MessageStatus.SENT, sent.status());
        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("Texto", all.get(1).body());
    }

    @Test
    void installationWithoutArchiveSendsNoCopy() {
        MessageSenders.create(sentMessages, false, false).send("ana@gmail.com", "Oi", "Texto");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void blocksInvalidAddressesWithoutCopy() {
        MessageSender sender = MessageSenders.create(sentMessages, false, true);
        for (String address : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com",
                "ana@.com", "ana@gmail.", "")) {
            assertEquals(MessageStatus.BLOCKED, sender.send(address, "Oi", "Texto").status(), address);
        }
        List<Message> all = sentMessages.all();
        assertEquals(8, all.size());
        assertTrue(all.stream().allMatch(m -> m.status() == MessageStatus.BLOCKED));
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void testModeRedirectsWithoutArchiveCopy() {
        MessageSenders.create(sentMessages, true, true).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
    }

    @Test
    void testModeStillBlocksInvalidAddressKeepingTheOriginal() {
        Message blocked = MessageSenders.create(sentMessages, true, true).send("ana@gmail", "Oi", "Texto");

        assertEquals(MessageStatus.BLOCKED, blocked.status());
        assertEquals("ana@gmail", sentMessages.all().get(0).to());
        assertEquals("Oi", sentMessages.all().get(0).subject());
    }
}
