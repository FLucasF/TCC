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

    private EmailSender sender(MessagePolicy policy) {
        return new EmailSender(sentMessages, policy);
    }

    @Test
    void validAddressesNeedOneAtAndADottedName() {
        assertTrue(MessageRules.isValidAddress("ana@gmail.com"));
        assertFalse(MessageRules.isValidAddress("ana@gmail"));
        assertFalse(MessageRules.isValidAddress("ana.gmail.com"));
        assertFalse(MessageRules.isValidAddress("@gmail.com"));
        assertFalse(MessageRules.isValidAddress("a@b@gmail.com"));
        assertFalse(MessageRules.isValidAddress("ana@.com"));
        assertFalse(MessageRules.isValidAddress("ana@gmail."));
    }

    @Test
    void sentMessageGetsACopyForTheArchive() {
        sender(MessagePolicy.standard(false)).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Oi", all.get(1).subject());
        assertEquals("Texto", all.get(1).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() {
        Message message = sender(MessagePolicy.standard(false)).send("ana@gmail", "Oi", "Texto");

        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals(List.of(message), sentMessages.all());
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        sender(MessagePolicy.standard(true)).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
    }

    @Test
    void testModeStillBlocksWithTheOriginalAddress() {
        sender(MessagePolicy.standard(true)).send("ana.gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana.gmail.com", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void anInstallationCanLeaveTheArchiveOut() {
        MessagePolicy policy = new MessagePolicy(false).block(Scope.ALWAYS, MessageRules.invalidAddress());
        sender(policy).send("ana@gmail.com", "Oi", "Texto");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void testModeComesFromTheEnvironment() {
        EmailSender sender = sender(MessagePolicy.fromEnvironment(java.util.Map.of("HOTEL_TEST_MODE", "true")));
        sender.send("ana@gmail.com", "Oi", "Texto");

        assertEquals("testes@pousadamarealta.com.br", sentMessages.all().get(0).to());
    }
}
