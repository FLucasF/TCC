package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages);

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void sendValidEmailAndArchiveCopy() {
        emailSender.send("user@example.com", "Test Subject", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("user@example.com", messages.get(0).to());
        assertEquals("Test Subject", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Test Subject", messages.get(1).subject());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void blocksInvalidEmail() {
        emailSender.send("invalid@email", "Test Subject", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid@email", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void doesNotCreateArchiverForBlockedMessages() {
        emailSender.send("ana@gmail", "Test Subject", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void testModeRedirectsToTestEmail() {
        EmptyDatabase testDb = new EmptyDatabase();
        SentMessages testMessages = new SentMessages(testDb.dataSource());
        EmailSender testSender = new EmailSender(testMessages, true);

        testSender.send("user@example.com", "Test Subject", "Test body");

        List<Message> messages = testMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Test Subject", messages.get(0).subject());
        assertEquals("Destinatário original: user@example.com\nTest body", messages.get(0).body());
        assertEquals("SENT", messages.get(0).status());

        testDb.close();
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        EmptyDatabase testDb = new EmptyDatabase();
        SentMessages testMessages = new SentMessages(testDb.dataSource());
        EmailSender testSender = new EmailSender(testMessages, true);

        testSender.send("user@example.com", "Test Subject", "Test body");

        List<Message> messages = testMessages.all();
        assertEquals(1, messages.size());

        testDb.close();
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        EmptyDatabase testDb = new EmptyDatabase();
        SentMessages testMessages = new SentMessages(testDb.dataSource());
        EmailSender testSender = new EmailSender(testMessages, true);

        testSender.send("invalid@email", "Test Subject", "Test body");

        List<Message> messages = testMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("invalid@email", messages.get(0).to());

        testDb.close();
    }
}
