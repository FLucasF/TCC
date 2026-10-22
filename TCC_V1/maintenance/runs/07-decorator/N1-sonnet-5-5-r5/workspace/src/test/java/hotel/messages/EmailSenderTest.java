package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void close() {
        database.close();
    }

    private EmailSender sender(boolean testMode) {
        return new EmailSender(sentMessages, testMode, EmailRules.mareAlta());
    }

    @Test
    void sendsToTheGuestAndCopiesToTheArchive() {
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
    void blocksInvalidAddressesWithoutCopy() {
        for (String invalid : List.of("ana@gmail", "ana.gmail.com", "@gmail.com", "a@@b.com", "a@b@c.com",
                "a@.com", "a@gmail.", "")) {
            sender(false).send(invalid, "Olá", "texto");
        }

        List<Message> all = sentMessages.all();
        assertEquals(8, all.size());
        for (Message message : all) {
            assertEquals(MessageStatus.BLOCKED, message.status());
            assertEquals("Olá", message.subject());
        }
        assertEquals("ana@gmail", all.get(0).to());
    }

    @Test
    void testModeRedirectsWithoutCopy() {
        sender(true).send("ana@gmail.com", "Olá", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Olá", all.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\ntexto", all.get(0).body());
        assertEquals(MessageStatus.SENT, all.get(0).status());
    }

    @Test
    void testModeStillBlocksInvalidAddressesKeepingTheOriginal() {
        sender(true).send("ana@gmail", "Olá", "texto");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("ana@gmail", all.get(0).to());
        assertEquals("Olá", all.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
    }

    @Test
    void aRuleOnlyAppliesWhereItsScopeSays() {
        EmailSender noArchive = new EmailSender(sentMessages, false, List.of(
                new ScopedRule(RuleScope.ALWAYS, new BlockInvalidAddress()),
                new ScopedRule(RuleScope.TEST_MODE_ONLY, new CopyToArchive())));
        noArchive.send("ana@gmail.com", "Olá", "texto");

        assertEquals(1, sentMessages.all().size());
    }
}
