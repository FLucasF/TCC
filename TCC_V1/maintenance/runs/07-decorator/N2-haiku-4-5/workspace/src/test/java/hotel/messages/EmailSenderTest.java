package hotel.messages;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailSenderTest {

    private EmptyDatabase database;
    private SentMessages sentMessages;
    private EmailSender emailSender;

    @BeforeEach
    void setup() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource());
        emailSender = new EmailSender(sentMessages);
    }

    @AfterEach
    void cleanup() {
        database.close();
    }

    @Test
    void validEmailIsSent() {
        emailSender.send("guest@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("SENT", messages.get(1).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
    }

    @Test
    void invalidEmailIsBlocked() {
        emailSender.send("invalid-email", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("invalid-email", messages.get(0).to());
    }

    @Test
    void emailWithoutDomainIsBlocked() {
        emailSender.send("guest@example", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void emailWithoutAtIsBlocked() {
        emailSender.send("guest.example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }
}
