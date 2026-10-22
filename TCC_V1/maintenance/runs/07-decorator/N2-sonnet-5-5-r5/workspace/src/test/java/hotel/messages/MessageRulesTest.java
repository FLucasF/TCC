package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageRulesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final MessageSender real = new EmailSender(sentMessages);
    private final MessageSender normal =
            new ArchiveCopy(new ValidAddressOnly(real, sentMessages), "arquivo@pousadamarealta.com.br");
    private final MessageSender testMode = new ValidAddressOnly(
            new RedirectToTestAddress(real, "testes@pousadamarealta.com.br"), sentMessages);

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void sendsToAValidAddressAndKeepsACopyForTheArchive() {
        normal.send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
    }

    @Test
    void blocksInvalidAddressesWithoutACopy() {
        for (String address : List.of("ana@gmail", "ana.gmail.com", "a@@b.com", "@b.com", "a@.com", "a@b.")) {
            normal.send(address, "Oi", "texto");
        }

        List<Message> all = sentMessages.all();
        assertEquals(6, all.size());
        all.forEach(message -> assertEquals(MessageStatus.BLOCKED, message.status()));
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void testModeRedirectsWithoutArchiveCopy() {
        testMode.send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        testMode.send("ana@gmail", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("Oi", all.get(0).subject());
    }
}
