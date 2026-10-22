package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import hotel.messages.Message;
import hotel.messages.MessageRules;
import hotel.messages.SentMessages;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SentMessagesIntegrationTest {

    private final EmptyDatabase database = new EmptyDatabase();

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void normalModeCreatesArchiveCopy() {
        MessageRules rules = new MessageRules(new HashMap<>());
        SentMessages sentMessages = new SentMessages(database.dataSource(), rules);

        sentMessages.add("EMAIL", "guest@example.com", "Confirmation", "Your reservation is confirmed.");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("guest@example.com", all.get(0).to());
        assertEquals("SENT", all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Confirmation", all.get(1).subject());
        assertEquals("SENT", all.get(1).status());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        Map<String, String> env = new HashMap<>();
        env.put("HOTEL_TEST_MODE", "true");
        MessageRules rules = new MessageRules(env);
        SentMessages sentMessages = new SentMessages(database.dataSource(), rules);

        sentMessages.add("EMAIL", "guest@example.com", "Confirmation", "Your reservation is confirmed.");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Confirmation", all.get(0).subject());
        assertEquals("Destinatário original: guest@example.com\nYour reservation is confirmed.", all.get(0).body());
        assertEquals("SENT", all.get(0).status());
    }

    @Test
    void invalidEmailIsBlocked() {
        MessageRules rules = new MessageRules(new HashMap<>());
        SentMessages sentMessages = new SentMessages(database.dataSource(), rules);

        sentMessages.add("EMAIL", "invalid.email", "Confirmation", "Your reservation is confirmed.");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("invalid.email", all.get(0).to());
        assertEquals("BLOCKED", all.get(0).status());
    }

    @Test
    void invalidEmailInTestModeIsStillBlocked() {
        Map<String, String> env = new HashMap<>();
        env.put("HOTEL_TEST_MODE", "true");
        MessageRules rules = new MessageRules(env);
        SentMessages sentMessages = new SentMessages(database.dataSource(), rules);

        sentMessages.add("EMAIL", "invalid.email", "Confirmation", "Your reservation is confirmed.");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("invalid.email", all.get(0).to());
        assertEquals("BLOCKED", all.get(0).status());
    }
}
