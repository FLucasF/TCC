package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailSenderTest {

    @Test
    void validEmailsAreSent() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("ana@example.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Test", messages.get(0).subject());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Test", messages.get(1).subject());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void invalidEmailsAreBlocked() {
        SentMessages sentMessages = new SentMessages();
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("ana@@gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        for (Message message : messages) {
            assertEquals(MessageStatus.BLOCKED, message.status());
        }
    }
}
