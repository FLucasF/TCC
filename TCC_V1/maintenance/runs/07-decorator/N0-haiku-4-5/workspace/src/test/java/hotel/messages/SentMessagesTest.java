package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SentMessagesTest {

    private EmptyDatabase database;
    private SentMessages sentMessages;

    @BeforeEach
    void setUp() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource(), false);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void validEmailIsSent() {
        EmailSender sender = new EmailSender(sentMessages);
        sender.send("user@example.com", "Test Subject", "Test Body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("user@example.com", messages.get(0).to());
        assertEquals("SENT", messages.get(1).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Test Subject", messages.get(1).subject());
    }

    @Test
    void invalidEmailIsBlocked() {
        EmailSender sender = new EmailSender(sentMessages);
        sender.send("invalid-email", "Test Subject", "Test Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("invalid-email", messages.get(0).to());
    }

    @Test
    void emailWithoutDotIsInvalid() {
        EmailSender sender = new EmailSender(sentMessages);
        sender.send("user@example", "Test Subject", "Test Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void emailWithoutAtIsInvalid() {
        EmailSender sender = new EmailSender(sentMessages);
        sender.send("user.example.com", "Test Subject", "Test Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void emailWithMultipleAtIsInvalid() {
        EmailSender sender = new EmailSender(sentMessages);
        sender.send("user@@example.com", "Test Subject", "Test Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void testModeDoesNotCreateCopy() {
        SentMessages testModeSentMessages = new SentMessages(database.dataSource(), true);
        EmailSender sender = new EmailSender(testModeSentMessages);
        sender.send("user@example.com", "Original Subject", "Original Body");

        List<Message> messages = testModeSentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Original Subject", messages.get(0).subject());
        assertEquals("Destinatário original: user@example.com\nOriginal Body", messages.get(0).body());
    }

    @Test
    void testModeInvalidEmailIsStillBlocked() {
        SentMessages testModeSentMessages = new SentMessages(database.dataSource(), true);
        EmailSender sender = new EmailSender(testModeSentMessages);
        sender.send("invalid-email", "Subject", "Body");

        List<Message> messages = testModeSentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("invalid-email", messages.get(0).to());
    }
}
