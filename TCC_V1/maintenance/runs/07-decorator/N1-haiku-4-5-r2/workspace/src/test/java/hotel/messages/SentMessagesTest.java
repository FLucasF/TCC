package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SentMessagesTest {

    private EmptyDatabase database;
    private SentMessages sentMessages;

    private void setupNormalMode() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource(), false);
    }

    private void setupTestMode() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource(), true);
    }

    @AfterEach
    void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void validEmailsAreSent() {
        setupNormalMode();
        sentMessages.add("EMAIL", "ana@example.com", "Hello", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Hello", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Hello", messages.get(1).subject());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void invalidEmailsAreBlocked() {
        setupNormalMode();
        sentMessages.add("EMAIL", "ana", "Hello", "Test body");
        sentMessages.add("EMAIL", "ana@", "Hello", "Test body");
        sentMessages.add("EMAIL", "ana.gmail.com", "Hello", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(3, messages.size());
        for (Message msg : messages) {
            assertEquals("BLOCKED", msg.status());
        }
        assertEquals("ana", messages.get(0).to());
        assertEquals("ana@", messages.get(1).to());
        assertEquals("ana.gmail.com", messages.get(2).to());
    }

    @Test
    void blockedEmailsDoNotCreateArchiveCopy() {
        setupNormalMode();
        sentMessages.add("EMAIL", "invalid", "Hello", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void testModeRedirectsToTestEmail() {
        setupTestMode();
        sentMessages.add("EMAIL", "ana@example.com", "Hello", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Hello", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: ana@example.com\n"));
        assertTrue(messages.get(0).body().contains("Test body"));
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        setupTestMode();
        sentMessages.add("EMAIL", "ana@example.com", "Hello", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        setupTestMode();
        sentMessages.add("EMAIL", "invalid", "Hello", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("invalid", messages.get(0).to());
    }
}
