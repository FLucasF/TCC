package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSendersTest {

    private final SentMessages sentMessages = new SentMessages();

    private EmailSender sender(boolean testMode, boolean archive) {
        return EmailSenders.assemble(sentMessages, testMode, archive);
    }

    @Test
    void validatesAddresses() {
        assertTrue(ValidatingEmailSender.isValid("ana@gmail.com"));
        assertFalse(ValidatingEmailSender.isValid("ana@gmail"));
        assertFalse(ValidatingEmailSender.isValid("ana.gmail.com"));
        assertFalse(ValidatingEmailSender.isValid("@gmail.com"));
        assertFalse(ValidatingEmailSender.isValid("a@@gmail.com"));
        assertFalse(ValidatingEmailSender.isValid("a@b@gmail.com"));
        assertFalse(ValidatingEmailSender.isValid("ana@.com"));
        assertFalse(ValidatingEmailSender.isValid("ana@gmail."));
    }

    @Test
    void normalModeSendsAndArchivesACopy() {
        sender(false, true).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        Message message = sender(false, true).send("ana@gmail", "Oi", "texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals(List.of(message), sentMessages.all());
    }

    @Test
    void installationWithoutArchiveSendsNoCopy() {
        sender(false, false).send("ana@gmail.com", "Oi", "texto");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void testModeRedirectsAndSkipsTheArchive() {
        sender(true, true).send("ana@gmail.com", "Oi", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        Message message = sender(true, true).send("ana.gmail.com", "Oi", "texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals("ana.gmail.com", message.to());
        assertEquals("Oi", message.subject());
    }
}
