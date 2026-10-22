package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        SentMessages sentMessages = new SentMessages(database.dataSource(), false);
        sentMessages.add("EMAIL", "invalid-email", "Subject", "Body");
        sentMessages.add("EMAIL", "ana@gmail", "Subject", "Body");
        sentMessages.add("EMAIL", "ana.gmail.com", "Subject", "Body");
        sentMessages.add("EMAIL", "ana@", "Subject", "Body");
        sentMessages.add("EMAIL", "@example.com", "Subject", "Body");

        List<Message> all = sentMessages.all();
        assertEquals(5, all.size());
        for (Message msg : all) {
            assertEquals("BLOCKED", msg.status());
        }
    }

    @Test
    void acceptsValidEmails() {
        SentMessages sentMessages = new SentMessages(database.dataSource(), false);
        Message msg1 = sentMessages.add("EMAIL", "ana@example.com", "Subject", "Body");
        Message msg2 = sentMessages.add("EMAIL", "user@domain.co.uk", "Subject", "Body");

        List<Message> all = sentMessages.all();
        assertEquals(4, all.size());
        assertEquals("SENT", msg1.status());
        assertEquals("SENT", msg2.status());
        assertEquals("ana@example.com", all.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("user@domain.co.uk", all.get(2).to());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(3).to());
    }

    @Test
    void createsArchiveEachTimeAnEmailIsSent() {
        SentMessages sentMessages = new SentMessages(database.dataSource(), false);
        sentMessages.add("EMAIL", "guest@example.com", "Confirmation", "Thanks for booking");

        List<Message> all = sentMessages.all();
        assertEquals(2, all.size());
        assertEquals("guest@example.com", all.get(0).to());
        assertEquals("Confirmation", all.get(0).subject());
        assertEquals("SENT", all.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", all.get(1).to());
        assertEquals("[Cópia] Confirmation", all.get(1).subject());
        assertEquals("SENT", all.get(1).status());
    }

    @Test
    void inTestModeRedirectsToTestEmail() {
        SentMessages sentMessages = new SentMessages(database.dataSource(), true);
        sentMessages.add("EMAIL", "guest@example.com", "Confirmation", "Thanks for booking");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("testes@pousadamarealta.com.br", all.get(0).to());
        assertEquals("[TESTE] Confirmation", all.get(0).subject());
        assertTrue(all.get(0).body().contains("Destinatário original: guest@example.com"));
        assertTrue(all.get(0).body().contains("Thanks for booking"));
        assertEquals("SENT", all.get(0).status());
    }

    @Test
    void inTestModeBlockedEmailsAreNotRedirected() {
        SentMessages sentMessages = new SentMessages(database.dataSource(), true);
        sentMessages.add("EMAIL", "invalid@email", "Subject", "Body");

        List<Message> all = sentMessages.all();
        assertEquals(1, all.size());
        assertEquals("invalid@email", all.get(0).to());
        assertEquals("BLOCKED", all.get(0).status());
    }
}
