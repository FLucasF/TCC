package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSendersTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void close() {
        database.close();
    }

    private EmailSender sender(boolean testMode, boolean archiveCopy) {
        return EmailSenders.create(sentMessages, new MessagingSettings(testMode, archiveCopy));
    }

    @Test
    void aSentMessageGetsACopyForTheArchiveRightAfterIt() {
        sender(false, true).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void anInstallationWithoutArchiveSendsNoCopy() {
        sender(false, false).send("ana@gmail.com", "Oi", "texto");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void aMistypedAddressIsBlockedWithoutCopy() {
        for (String address : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@.com", "a@b.", "")) {
            Message message = sender(false, true).send(address, "Oi", "texto");
            assertEquals(MessageStatus.BLOCKED, message.status(), address);
        }
        assertEquals(7, sentMessages.all().size());
        assertEquals("ana@gmail", sentMessages.all().get(0).to());
    }

    @Test
    void inTestModeTheMessageGoesToTheTestInboxWithoutCopy() {
        sender(true, true).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void inTestModeAMistypedAddressIsStillBlockedWithTheOriginalAddress() {
        Message message = sender(true, true).send("ana@gmail", "Oi", "texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals("ana@gmail", message.to());
        assertEquals("Oi", message.subject());
        assertEquals(1, sentMessages.all().size());
    }
}
