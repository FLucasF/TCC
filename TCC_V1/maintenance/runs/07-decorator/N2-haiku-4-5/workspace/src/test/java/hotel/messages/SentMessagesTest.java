package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SentMessagesTest {

    @Test
    void invalidEmailsAreBlocked() {
        SentMessages messages = new SentMessages(new EmailConfiguration(false, false));

        messages.add("EMAIL", "ana@gmail", "Subject", "Body");
        messages.add("EMAIL", "ana.gmail.com", "Subject", "Body");
        messages.add("EMAIL", "valid@example.com", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(3, all.size());

        assertEquals(MessageStatus.BLOCKED, all.get(0).status());
        assertEquals("ana@gmail", all.get(0).to());

        assertEquals(MessageStatus.BLOCKED, all.get(1).status());
        assertEquals("ana.gmail.com", all.get(1).to());

        assertEquals(MessageStatus.SENT, all.get(2).status());
        assertEquals("valid@example.com", all.get(2).to());
    }

    @Test
    void testModeRedirectsToTestAddress() {
        SentMessages messages = new SentMessages(new EmailConfiguration(true, false));

        messages.add("EMAIL", "guest@example.com", "Reservation", "Details");

        List<Message> all = messages.all();
        assertEquals(1, all.size());

        Message message = all.get(0);
        assertEquals(MessageStatus.SENT, message.status());
        assertEquals("testes@pousadamarealta.com.br", message.to());
        assertEquals("[TESTE] Reservation", message.subject());
        assertTrue(message.body().startsWith("Destinatário original: guest@example.com\n"));
        assertTrue(message.body().contains("Details"));
    }

    @Test
    void testModeBlocksInvalidEmailsWithoutCopy() {
        SentMessages messages = new SentMessages(new EmailConfiguration(true, false));

        messages.add("EMAIL", "invalid@email", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(1, all.size());

        Message message = all.get(0);
        assertEquals(MessageStatus.BLOCKED, message.status());
        assertEquals("invalid@email", message.to());
    }

    @Test
    void archivesCopyOfValidEmailsWhenNotInTestMode() {
        SentMessages messages = new SentMessages(new EmailConfiguration(false, true));

        messages.add("EMAIL", "guest@example.com", "Confirmation", "Booking details");

        List<Message> all = messages.all();
        assertEquals(2, all.size());

        Message original = all.get(0);
        assertEquals(MessageStatus.SENT, original.status());
        assertEquals("guest@example.com", original.to());
        assertEquals("Confirmation", original.subject());

        Message copy = all.get(1);
        assertEquals(MessageStatus.SENT, copy.status());
        assertEquals("arquivo@pousadamarealta.com.br", copy.to());
        assertEquals("[Cópia] Confirmation", copy.subject());
        assertEquals("Booking details", copy.body());
    }

    @Test
    void noCopyForBlockedEmails() {
        SentMessages messages = new SentMessages(new EmailConfiguration(false, true));

        messages.add("EMAIL", "invalid@email", "Subject", "Body");

        List<Message> all = messages.all();
        assertEquals(1, all.size());

        Message message = all.get(0);
        assertEquals(MessageStatus.BLOCKED, message.status());
    }

    @Test
    void noCopyInTestMode() {
        SentMessages messages = new SentMessages(new EmailConfiguration(true, true));

        messages.add("EMAIL", "guest@example.com", "Confirmation", "Booking details");

        List<Message> all = messages.all();
        assertEquals(1, all.size());

        Message message = all.get(0);
        assertEquals(MessageStatus.SENT, message.status());
        assertEquals("testes@pousadamarealta.com.br", message.to());
        assertTrue(message.body().startsWith("Destinatário original: guest@example.com\n"));
    }

    @Test
    void archiveConfigCanBeDisabled() {
        SentMessages messages = new SentMessages(new EmailConfiguration(false, false));

        messages.add("EMAIL", "guest@example.com", "Confirmation", "Booking details");

        List<Message> all = messages.all();
        assertEquals(1, all.size());

        Message message = all.get(0);
        assertEquals(MessageStatus.SENT, message.status());
        assertEquals("guest@example.com", message.to());
    }
}
