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
        return new EmailSender(sentMessages, MessageRules.standard(testMode));
    }

    @Test
    void aValidMessageIsSentAndCopiedToTheArchiveRightAfter() {
        sender(false).send("ana@gmail.com", "Olá", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("ana@gmail.com", all.get(0).to());
        assertEquals("Olá", all.get(0).subject());
        assertEquals(MessageStatus.SENT, all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Olá", all.get(1).subject());
        assertEquals("texto", all.get(1).body());
        assertEquals(MessageStatus.SENT, all.get(1).status());
    }

    @Test
    void anInvalidAddressIsBlockedWithoutACopy() {
        sender(false).send("ana@gmail", "Olá", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void testModeRedirectsWithoutACopy() {
        sender(true).send("ana@gmail.com", "Olá", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Olá", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksAnInvalidAddressKeepingTheOriginal() {
        sender(true).send("ana.gmail.com", "Olá", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana.gmail.com", all.get(0).to());
        assertEquals("Olá", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void validatesTheAddressFormat() {
        assertTrue(MessageRules.isValidAddress("ana@gmail.com"));
        assertTrue(MessageRules.isValidAddress("a@b.c.d"));
        assertFalse(MessageRules.isValidAddress("ana@gmail"));
        assertFalse(MessageRules.isValidAddress("ana.gmail.com"));
        assertFalse(MessageRules.isValidAddress("@gmail.com"));
        assertFalse(MessageRules.isValidAddress("a@@gmail.com"));
        assertFalse(MessageRules.isValidAddress("a@b@gmail.com"));
        assertFalse(MessageRules.isValidAddress("ana@.com"));
        assertFalse(MessageRules.isValidAddress("ana@gmail."));
    }

    @Test
    void aRuleCanBeLeftOutOfAnInstallation() {
        new EmailSender(sentMessages, List.of(MessageRules.blockInvalidAddress()))
                .send("ana@gmail.com", "Olá", "texto");

        assertEquals(1, sentMessages.all().size());
    }

    @Test
    void aRuleInTheOtherModeDoesNothing() {
        MessageRule rule = MessageRule.in(RuleMode.ONLY_IN_TEST, false, MessageRules.redirectToTestAddress());
        new EmailSender(sentMessages, List.of(rule)).send("ana@gmail.com", "Olá", "texto");

        assertEquals("ana@gmail.com", sentMessages.all().get(0).to());
    }
}
