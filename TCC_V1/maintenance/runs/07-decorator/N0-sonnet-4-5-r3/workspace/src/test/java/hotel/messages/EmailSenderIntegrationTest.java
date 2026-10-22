package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderIntegrationTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void normalModeSendsToRecipientAndArchive() {
        MessageProcessor processor = new MessageProcessor(new EmailValidator(), false);
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("carla@example.com", "Assunto", "Corpo da mensagem");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());

        assertEquals("carla@example.com", messages.get(0).to());
        assertEquals("Assunto", messages.get(0).subject());
        assertEquals("Corpo da mensagem", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Assunto", messages.get(1).subject());
        assertEquals("Corpo da mensagem", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void testModeSendsOnlyToTestAddress() {
        MessageProcessor processor = new MessageProcessor(new EmailValidator(), true);
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("carla@example.com", "Assunto", "Corpo da mensagem");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Assunto", messages.get(0).subject());
        assertEquals("Destinatário original: carla@example.com\nCorpo da mensagem", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void invalidEmailIsBlocked() {
        MessageProcessor processor = new MessageProcessor(new EmailValidator(), false);
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("carla@gmail", "Assunto", "Corpo da mensagem");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("carla@gmail", messages.get(0).to());
        assertEquals("Assunto", messages.get(0).subject());
        assertEquals("Corpo da mensagem", messages.get(0).body());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void invalidEmailIsBlockedInTestMode() {
        MessageProcessor processor = new MessageProcessor(new EmailValidator(), true);
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("carla.example.com", "Assunto", "Corpo da mensagem");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("carla.example.com", messages.get(0).to());
        assertEquals("Assunto", messages.get(0).subject());
        assertEquals("Corpo da mensagem", messages.get(0).body());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
