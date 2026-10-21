package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final SentMessages sentMessages = new SentMessages();

    private EmailSender sender(boolean testMode) {
        return new EmailSender(sentMessages, MessageRules.pousadaMareAlta(), testMode);
    }

    @Test
    void aValidAddressIsSentAndCopiedToTheArchive() {
        sender(false).send("ana@gmail.com", "Oi", "Texto");

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
    void anInvalidAddressIsBlockedWithoutCopy() {
        for (String address : new String[] {"ana@gmail", "ana.gmail.com", "@gmail.com", "a@b@c.com", "a@.com", "a@gmail.", ""}) {
            assertFalse(ValidAddressRule.isValid(address), address);
        }
        sender(false).send("ana@gmail", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void testModeRedirectsWithoutArchiveCopy() {
        sender(true).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        sender(true).send("ana.gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana.gmail.com", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
        assertTrue(all.get(0).status() == MessageStatus.BLOCKED);
    }

    @Test
    void aSenderWithoutRulesSendsAsItIs() {
        new EmailSender(sentMessages).send("qualquer", "Oi", "Texto");

        assertEquals(1, sentMessages.all().size());
        assertEquals(MessageStatus.SENT, sentMessages.all().get(0).status());
    }
}
