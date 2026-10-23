package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageProcessingTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void blocksInvalidEmailAddresses() {
        EmailSender sender = new EmailSender(sentMessages, List.of(new EmailValidator()));

        sender.send("invalid", "Subject", "Body");
        sender.send("missing@domain", "Subject", "Body");
        sender.send("double@@at.com", "Subject", "Body");
        sender.send("valid@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
        assertEquals(MessageStatus.BLOCKED, messages.get(1).status());
        assertEquals(MessageStatus.BLOCKED, messages.get(2).status());
        assertEquals(MessageStatus.SENT, messages.get(3).status());
    }

    @Test
    void redirectsMessagesInTestMode() {
        EmailSender sender = new EmailSender(
                sentMessages,
                List.of(new EmailValidator(), new TestModeRedirector(true)));

        sender.send("guest@example.com", "Welcome", "Hello guest!");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Welcome", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: guest@example.com\n"));
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void doesNotRedirectBlockedMessages() {
        EmailSender sender = new EmailSender(
                sentMessages,
                List.of(new EmailValidator(), new TestModeRedirector(true)));

        sender.send("invalid", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid", messages.get(0).to());
        assertEquals("Subject", messages.get(0).subject());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void createsArchiveCopyInProductionMode() {
        EmailSender sender = new EmailSender(
                sentMessages,
                List.of(new EmailValidator(), new ArchiveCopier(false)));

        sender.send("guest@example.com", "Booking confirmed", "Your booking is confirmed.");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("Booking confirmed", messages.get(0).subject());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Booking confirmed", messages.get(1).subject());
        assertEquals("Your booking is confirmed.", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void doesNotCreateArchiveCopyInTestMode() {
        EmailSender sender = new EmailSender(
                sentMessages,
                List.of(new EmailValidator(), new TestModeRedirector(true), new ArchiveCopier(true)));

        sender.send("guest@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
    }

    @Test
    void doesNotCreateArchiveCopyForBlockedMessages() {
        EmailSender sender = new EmailSender(
                sentMessages,
                List.of(new EmailValidator(), new ArchiveCopier(false)));

        sender.send("invalid", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void processorsWorkTogetherInProductionMode() {
        EmailSender sender = new EmailSender(
                sentMessages,
                List.of(new EmailValidator(), new TestModeRedirector(false), new ArchiveCopier(false)));

        sender.send("guest@example.com", "Confirmation", "Your stay is confirmed.");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
    }

    @Test
    void processorsWorkTogetherInTestMode() {
        EmailSender sender = new EmailSender(
                sentMessages,
                List.of(new EmailValidator(), new TestModeRedirector(true), new ArchiveCopier(true)));

        sender.send("guest@example.com", "Confirmation", "Your stay is confirmed.");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Confirmation", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: guest@example.com\n"));
    }
}
