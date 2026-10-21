package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final SentMessages sentMessages = new SentMessages();
    private final EmailSender emailSender = new EmailSender(sentMessages);

    @Test
    void sendsValidEmailsWithArchiveCopy() {
        emailSender.send("guest@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("Subject", messages.get(0).subject());
        assertEquals("Body", messages.get(0).body());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("SENT", messages.get(1).status());
        assertEquals("[Cópia] Subject", messages.get(1).subject());
        assertEquals("Body", messages.get(1).body());
    }

    @Test
    void blocksInvalidEmails() {
        emailSender.send("invalid-email", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid-email", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("Subject", messages.get(0).subject());
    }

    @Test
    void blocksEmailsWithoutDot() {
        emailSender.send("test@example", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void blocksEmailsWithoutAt() {
        emailSender.send("test.example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void sendsToTestEmailInTestMode() {
        SentMessages testMessages = new SentMessages();
        EmailSender testSender = new EmailSender(testMessages, () -> true);

        testSender.send("guest@example.com", "Subject", "Body");

        List<Message> messages = testMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("[TESTE] Subject", messages.get(0).subject());
        assertEquals("Destinatário original: guest@example.com\nBody", messages.get(0).body());
    }

    @Test
    void blocksInvalidEmailsInTestMode() {
        SentMessages testMessages = new SentMessages();
        EmailSender testSender = new EmailSender(testMessages, () -> true);

        testSender.send("invalid-email", "Subject", "Body");

        List<Message> messages = testMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid-email", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }
}
