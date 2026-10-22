package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TestModeTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void redirectsToTestAddressInTestMode() {
        EmailSender sender = new EmailSender(sentMessages, List.of(new EmailValidationRule(), new TestModeRule()));

        sender.send("ana@example.com", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Assunto", messages.get(0).subject());
        assertEquals("Destinatário original: ana@example.com\nCorpo", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void doesNotCreateArchiveCopyInTestMode() {
        EmailSender sender = new EmailSender(sentMessages, List.of(new EmailValidationRule(), new TestModeRule()));

        sender.send("ana@example.com", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
    }

    @Test
    void blockedMessagesKeepOriginalAddressInTestMode() {
        EmailSender sender = new EmailSender(sentMessages, List.of(new EmailValidationRule(), new TestModeRule()));

        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("ana@gmail", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
