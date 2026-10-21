package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailRulesTest {

    @Test
    void blocksInvalidEmailAddresses() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");
        sender.send("ana@", "Test", "Body");
        sender.send("ana@@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(5, messages.size());
        for (Message msg : messages) {
            assertEquals(MessageStatus.BLOCKED, msg.status());
        }
    }

    @Test
    void allowsValidEmailAddresses() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size()); // original + archive copy
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void testModeRedirectsToTestAddress() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("guest@example.com", "Reservation", "Details");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size()); // no archive copy in test mode
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Reservation", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: guest@example.com\n"));
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void testModeDoesNotRedirectBlockedMessages() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("invalid@email", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid@email", messages.get(0).to()); // keeps original address
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void normalModeCreatesArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("guest@example.com", "Reservation", "Details");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("Reservation", messages.get(0).subject());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reservation", messages.get(1).subject());
        assertEquals("Details", messages.get(1).body());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("guest@example.com", "Reservation", "Details");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size()); // only the redirected message, no archive
    }

    @Test
    void blockedMessagesDoNotCreateArchiveCopy() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("invalid@email", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size()); // only the blocked message
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
