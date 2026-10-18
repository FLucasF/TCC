package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final SentMessages sentMessages = new SentMessages();

    @Test
    void normalModeSendsAndArchivesACopyRightAfter() {
        new EmailSender(sentMessages, false).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("Texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void invalidAddressesAreBlockedAndNotArchived() {
        EmailSender sender = new EmailSender(sentMessages, false);
        for (String bad : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com", "a@.com", "a@gmail.", "")) {
            sender.send(bad, "Oi", "Texto");
        }

        assertEquals(8, sentMessages.all().size());
        sentMessages.all().forEach(m -> assertEquals(MessageStatus.BLOCKED, m.status()));
        assertEquals("ana@gmail", sentMessages.all().get(0).to());
    }

    @Test
    void testModeRedirectsWithoutArchiving() {
        new EmailSender(sentMessages, true).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        new EmailSender(sentMessages, true).send("ana@gmail", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }
}
