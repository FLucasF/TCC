package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private EmailSender sender(List<ScopedRule> rules, boolean testMode) {
        return new EmailSender(sentMessages, rules, testMode);
    }

    @Test
    void sendsAndArchivesACopyOutsideTestMode() {
        sender(MessageRules.ofTheInn(), false).send("ana@gmail.com", "Oi", "Texto");

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
    void blocksInvalidAddressesWithoutACopy() {
        for (String address : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@b@c.com", "a@.com", "a@gmail.", "")) {
            sender(MessageRules.ofTheInn(), false).send(address, "Oi", "Texto");
        }

        List<Message> all = sentMessages.all();
        assertEquals(7, all.size());
        assertTrue(all.stream().allMatch(message -> message.status() == MessageStatus.BLOCKED));
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void testModeRedirectsWithoutACopy() {
        sender(MessageRules.ofTheInn(), true).send("ana@gmail.com", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Oi", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\nTexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        sender(MessageRules.ofTheInn(), true).send("ana@gmail", "Oi", "Texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals("Oi", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void anInstallationCanLeaveTheArchiveOut() {
        sender(List.of(new ScopedRule(RuleScope.ALWAYS, new ValidAddressRule())), false)
                .send("ana@gmail.com", "Oi", "Texto");

        assertEquals(1, sentMessages.all().size());
    }
}
