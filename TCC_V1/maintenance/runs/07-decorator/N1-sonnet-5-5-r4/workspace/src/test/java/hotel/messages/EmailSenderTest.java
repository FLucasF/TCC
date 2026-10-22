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
    private final EmailSender normal = new EmailSender(sentMessages, MessageRules.marealta(false));
    private final EmailSender testMode = new EmailSender(sentMessages, MessageRules.marealta(true));

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void validAddressesHaveOneAtAndADotInTheMiddleOfTheName() {
        assertTrue(BlockInvalidAddress.isValid("ana@gmail.com"));
        assertFalse(BlockInvalidAddress.isValid("ana@gmail"));
        assertFalse(BlockInvalidAddress.isValid("ana.gmail.com"));
        assertFalse(BlockInvalidAddress.isValid("@gmail.com"));
        assertFalse(BlockInvalidAddress.isValid("ana@@gmail.com"));
        assertFalse(BlockInvalidAddress.isValid("a@b@gmail.com"));
        assertFalse(BlockInvalidAddress.isValid("ana@.com"));
        assertFalse(BlockInvalidAddress.isValid("ana@gmail."));
        assertFalse(BlockInvalidAddress.isValid(null));
    }

    @Test
    void aSentMessageGetsACopyForTheArchiveRightAfterIt() {
        normal.send("ana@gmail.com", "Oi", "Texto");

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
    void aMessageToAnInvalidAddressIsBlockedWithoutCopy() {
        normal.send("ana@gmail", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void inTestModeTheMessageGoesToTheTestAddressWithoutCopy() {
        testMode.send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void inTestModeAnInvalidAddressIsBlockedWithTheOriginalAddress() {
        testMode.send("ana.gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana.gmail.com", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }
}
