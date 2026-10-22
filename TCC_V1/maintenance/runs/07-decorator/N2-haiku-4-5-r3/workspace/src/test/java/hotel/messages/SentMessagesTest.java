package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SentMessagesTest {

    private final EmptyDatabase database = new EmptyDatabase();

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void normalModeCreatesOriginalAndArchiveCopy() {
        SentMessages sentMessages = new SentMessages(database.dataSource(), new SendingRules(null));
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("carla@example.com", "Reserva confirmada", "Seus dados...");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("carla@example.com", messages.get(0).to());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("SENT", messages.get(1).status());
        assertEquals("[Cópia] Reserva confirmada", messages.get(1).subject());
    }

    @Test
    void testModeRedirectsToTestEmailAndIgnoresArchiveCopy() {
        SentMessages sentMessages = new SentMessages(database.dataSource(), new SendingRules("true"));
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("carla@example.com", "Reserva confirmada", "Seus dados...");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("SENT", messages.get(0).status());
        assertEquals("[TESTE] Reserva confirmada", messages.get(0).subject());
        assertEquals("Destinatário original: carla@example.com\nSeus dados...", messages.get(0).body());
    }

    @Test
    void invalidEmailIsBlockedInBothModes() {
        SentMessages normalMode = new SentMessages(database.dataSource(), new SendingRules(null));
        EmailSender sender = new EmailSender(normalMode);

        sender.send("carla@gmail", "Assunto", "Corpo");

        List<Message> messages = normalMode.all();
        assertEquals(1, messages.size());
        assertEquals("carla@gmail", messages.get(0).to());
        assertEquals("BLOCKED", messages.get(0).status());

        database.close();
        EmptyDatabase testModeDb = new EmptyDatabase();
        SentMessages testMode = new SentMessages(testModeDb.dataSource(), new SendingRules("true"));
        EmailSender testSender = new EmailSender(testMode);

        testSender.send("carla@gmail", "Assunto", "Corpo");

        List<Message> testMessages = testMode.all();
        assertEquals(1, testMessages.size());
        assertEquals("carla@gmail", testMessages.get(0).to());
        assertEquals("BLOCKED", testMessages.get(0).status());
        testModeDb.close();
    }

    @Test
    void invalidEmailPreservesOriginalAddressInBlockedMessage() {
        SentMessages sentMessages = new SentMessages(database.dataSource(), new SendingRules(null));
        EmailSender sender = new EmailSender(sentMessages);

        sender.send("carla@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("carla@gmail", messages.get(0).to());
    }
}
