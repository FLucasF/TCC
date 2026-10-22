package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class EmailSenderTest {

    private EmptyDatabase database;
    private SentMessages sentMessages;

    @BeforeEach
    void resetDatabase() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource());
    }

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void blocksInvalidEmailAddresses() {
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");
        sender.send("ana@", "Test", "Body");
        sender.send("ana@domain", "Test", "Body");
        sender.send("", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(6, messages.size());
        for (Message message : messages) {
            assertEquals("BLOCKED", message.status());
        }
    }

    @Test
    void allowsValidEmailAddresses() {
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("ana@gmail.com", "Test", "Body");
        sender.send("test@example.com.br", "Test", "Body");
        sender.send("user.name@domain.co.uk", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(6, messages.size());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("SENT", messages.get(1).status());
        assertEquals("SENT", messages.get(2).status());
        assertEquals("SENT", messages.get(3).status());
        assertEquals("SENT", messages.get(4).status());
        assertEquals("SENT", messages.get(5).status());
    }

    @Test
    void sendsArchiveCopyInNormalMode() {
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("guest@example.com", "Reservation Confirmed", "Your reservation is confirmed.");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("guest@example.com", messages.get(0).to());
        assertEquals("Reservation Confirmed", messages.get(0).subject());
        assertEquals("Your reservation is confirmed.", messages.get(0).body());
        assertEquals("SENT", messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Reservation Confirmed", messages.get(1).subject());
        assertEquals("Your reservation is confirmed.", messages.get(1).body());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void redirectsToTestAddressInTestMode() {
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("guest@example.com", "Reservation Confirmed", "Your reservation is confirmed.");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Reservation Confirmed", messages.get(0).subject());
        assertEquals("Destinatário original: guest@example.com\nYour reservation is confirmed.",
                messages.get(0).body());
        assertEquals("SENT", messages.get(0).status());
    }

    @Test
    void doesNotSendArchiveCopyInTestMode() {
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("guest@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
    }

    @Test
    void blocksInvalidEmailsInTestMode() {
        EmailSender sender = new EmailSender(sentMessages, true);

        sender.send("invalid@email", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("invalid@email", messages.get(0).to());
    }

    @Test
    void doesNotSendArchiveCopyForBlockedMessages() {
        EmailSender sender = new EmailSender(sentMessages, false);

        sender.send("invalid@email", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }
}
