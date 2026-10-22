package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private EmptyDatabase database;
    private SentMessages sentMessages;
    private EmailSender emailSender;

    @BeforeEach
    void setup() {
        database = new EmptyDatabase();
        sentMessages = new SentMessages(database.dataSource());
    }

    @AfterEach
    void cleanup() {
        database.close();
    }

    @Test
    void validatesEmailAddresses() {
        emailSender = new EmailSender(sentMessages, false);

        emailSender.send("ana@gmail.com", "Test", "Body");
        emailSender.send("ana@gmail", "Test", "Body");
        emailSender.send("ana.gmail.com", "Test", "Body");
        emailSender.send("@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(5, messages.size());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("SENT", messages.get(1).status()); // archive copy
        assertEquals("BLOCKED", messages.get(2).status());
        assertEquals("BLOCKED", messages.get(3).status());
        assertEquals("BLOCKED", messages.get(4).status());
    }

    @Test
    void createsArchiveCopyForValidEmails() {
        emailSender = new EmailSender(sentMessages, false);

        emailSender.send("ana@gmail.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("Subject", messages.get(0).subject());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Subject", messages.get(1).subject());
        assertEquals("SENT", messages.get(1).status());
    }

    @Test
    void doesNotCreateArchiveCopyForBlockedEmails() {
        emailSender = new EmailSender(sentMessages, false);

        emailSender.send("ana@gmail", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void redirectsToTestAddressInTestMode() {
        emailSender = new EmailSender(sentMessages, true);

        emailSender.send("ana@gmail.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Subject", messages.get(0).subject());
        assertEquals("Destinatário original: ana@gmail.com\n\nBody", messages.get(0).body());
        assertEquals("SENT", messages.get(0).status());
    }

    @Test
    void blocksInvalidEmailsEvenInTestMode() {
        emailSender = new EmailSender(sentMessages, true);

        emailSender.send("ana@gmail", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).status());
        assertEquals("ana@gmail", messages.get(0).to());
    }
}
