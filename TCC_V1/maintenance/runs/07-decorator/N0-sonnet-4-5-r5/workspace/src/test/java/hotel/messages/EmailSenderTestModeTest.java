package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSenderTestModeTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());
    private final EmailSender emailSender = new EmailSender(sentMessages, true);

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void redirectsToTestEmailInTestMode() {
        emailSender.send("guest@example.com", "Original Subject", "Original body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());

        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Original Subject", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: guest@example.com\n"));
        assertTrue(messages.get(0).body().contains("Original body"));
        assertEquals("SENT", messages.get(0).status());
    }

    @Test
    void blocksInvalidEmailEvenInTestMode() {
        emailSender.send("invalid.email", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("invalid.email", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());
    }

    @Test
    void doesNotCreateArchiveCopyInTestMode() {
        emailSender.send("guest@example.com", "Subject", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
    }
}
