package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final SentMessages sent = new SentMessages();

    @Test
    void validAddressesNeedOneAtSomethingBeforeAndADotInTheMiddleOfTheDomain() {
        assertTrue(MessageRules.isValidAddress("ana@gmail.com"));
        assertFalse(MessageRules.isValidAddress("ana@gmail"));
        assertFalse(MessageRules.isValidAddress("ana.gmail.com"));
        assertFalse(MessageRules.isValidAddress("@gmail.com"));
        assertFalse(MessageRules.isValidAddress("a@b@gmail.com"));
        assertFalse(MessageRules.isValidAddress("ana@.com"));
        assertFalse(MessageRules.isValidAddress("ana@gmail."));
        assertFalse(MessageRules.isValidAddress(null));
    }

    @Test
    void liveModeSendsAndArchivesACopyRightAfter() {
        Message message = new EmailSender(sent).send("ana@gmail.com", "Oi", "Texto");

        assertEquals(MessageStatus.SENT, message.status());
        List<Message> all = sent.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("Texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        Message message = new EmailSender(sent).send("ana@gmail", "Oi", "Texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals(1, sent.all().size());
        assertEquals("ana@gmail", sent.all().get(0).to());
    }

    @Test
    void testModeRedirectsWithoutArchiveCopy() {
        new EmailSender(sent, true).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sent.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        Message message = new EmailSender(sent, true).send("ana.gmail.com", "Oi", "Texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals(1, sent.all().size());
        assertEquals("ana.gmail.com", message.to());
        assertEquals("Oi", message.subject());
        assertEquals("Texto", message.body());
    }
}
