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
    void sendsEmailAndCreatesArchiveCopy() {
        emailSender.send("guest@example.com", "Test Subject", "Test body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("Test Subject", messages.get(0).subject());
        assertEquals("Test body", messages.get(0).body());
        assertEquals("SENT", messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Test Subject", messages.get(1).subject());
        assertEquals("Test body", messages.get(1).body());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void blocksInvalidEmailWithoutAt() {
        emailSender.send("invalidemail.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalidemail.com", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void blocksInvalidEmailWithMultipleAt() {
        emailSender.send("invalid@@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid@@example.com", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void blocksInvalidEmailWithoutDomainDot() {
        emailSender.send("user@domain", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("user@domain", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void blocksInvalidEmailStartingWithAt() {
        emailSender.send("@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("@example.com", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void blocksBlankEmail() {
        emailSender.send("", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void acceptsValidEmails() {
        emailSender.send("user@example.com", "Subject 1", "Body 1");
        emailSender.send("name.surname@domain.co.uk", "Subject 2", "Body 2");
        emailSender.send("test+tag@gmail.com", "Subject 3", "Body 3");

        List<Message> messages = sentMessages.all();
        assertEquals(6, messages.size()); // 3 emails + 3 archive copies
        assertEquals("SENT", messages.get(0).status());
        assertEquals("SENT", messages.get(2).status());
        assertEquals("SENT", messages.get(4).status());
    }
}
