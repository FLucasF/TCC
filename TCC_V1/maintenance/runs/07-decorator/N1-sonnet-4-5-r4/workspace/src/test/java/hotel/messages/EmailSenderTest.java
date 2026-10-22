package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender sender = new EmailSender(sentMessages);

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void blocksInvalidEmailAddresses() {
        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");
        sender.send("ana@", "Test", "Body");
        sender.send("ana@@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(5, messages.size());
        for (Message message : messages) {
            assertEquals(MessageStatus.BLOCKED, message.status());
        }
    }

    @Test
    void sendsValidEmailAddresses() {
        Message sent = sender.send("ana@example.com", "Assunto", "Corpo");

        assertEquals(MessageStatus.SENT, sent.status());
        assertEquals("ana@example.com", sent.to());
        assertEquals("Assunto", sent.subject());
        assertEquals("Corpo", sent.body());
    }

    @Test
    void createsArchiveCopyInNormalMode() {
        sender.send("ana@example.com", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Assunto", messages.get(1).subject());
        assertEquals("Corpo", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void doesNotCreateArchiveCopyForBlockedMessages() {
        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
