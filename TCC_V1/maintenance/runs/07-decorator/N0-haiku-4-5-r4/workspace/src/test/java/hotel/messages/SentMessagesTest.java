package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SentMessagesTest {

    private final EmptyDatabase database = new EmptyDatabase();

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void blocksInvalidEmails() {
        MessageRules rules = new MessageRules();
        SentMessages sent = new SentMessages(database.dataSource(), rules);

        sent.add("EMAIL", "ana@gmail", "Test Subject", "Test Body");

        List<Message> messages = sent.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("ana@gmail", messages.get(0).to());
    }

    @Test
    void sendsValidEmailsAndCreatesArchiveCopy() {
        MessageRules rules = new MessageRules();
        SentMessages sent = new SentMessages(database.dataSource(), rules);

        sent.add("EMAIL", "carla@example.com", "Test Subject", "Test Body");

        List<Message> messages = sent.all();
        assertEquals(2, messages.size());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("carla@example.com", messages.get(0).to());
        assertEquals("Test Subject", messages.get(0).subject());
        assertEquals("SENT", messages.get(1).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Test Subject", messages.get(1).subject());
    }

    @Test
    void testModeRedirectsToTestsEmail() {
        MessageRules testRules = new MessageRules() {
            @Override
            public boolean isTestMode() {
                return true;
            }
        };
        SentMessages sent = new SentMessages(database.dataSource(), testRules);

        sent.add("EMAIL", "carla@example.com", "Test Subject", "Test Body");

        List<Message> messages = sent.all();
        assertEquals(1, messages.size());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Test Subject", messages.get(0).subject());
        assertEquals("Destinatário original: carla@example.com\nTest Body", messages.get(0).body());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        MessageRules testRules = new MessageRules() {
            @Override
            public boolean isTestMode() {
                return true;
            }
        };
        SentMessages sent = new SentMessages(database.dataSource(), testRules);

        sent.add("EMAIL", "carla@example.com", "Test Subject", "Test Body");

        List<Message> messages = sent.all();
        assertEquals(1, messages.size());
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        MessageRules testRules = new MessageRules() {
            @Override
            public boolean isTestMode() {
                return true;
            }
        };
        SentMessages sent = new SentMessages(database.dataSource(), testRules);

        sent.add("EMAIL", "ana@gmail", "Test Subject", "Test Body");

        List<Message> messages = sent.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("ana@gmail", messages.get(0).to());
    }
}
