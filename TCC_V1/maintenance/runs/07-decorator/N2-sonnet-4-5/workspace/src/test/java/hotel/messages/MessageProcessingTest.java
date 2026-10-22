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
    void invalidEmailAddressIsBlocked() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationHandler(),
                new TestModeHandler(false),
                new ArchiveCopyHandler(false)
        ));
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("ana@gmail", "Test", "Body");
        sender.send("ana.gmail.com", "Test", "Body");
        sender.send("@gmail.com", "Test", "Body");
        sender.send("ana@", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        for (Message msg : messages) {
            assertEquals(MessageStatus.BLOCKED, msg.status());
        }
    }

    @Test
    void validEmailAddressIsSent() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationHandler(),
                new TestModeHandler(false),
                new ArchiveCopyHandler(false)
        ));
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
    }

    @Test
    void testModeRedirectsToTestAddress() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationHandler(),
                new TestModeHandler(true),
                new ArchiveCopyHandler(true)
        ));
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("ana@gmail.com", "Reserva confirmada", "Olá Ana!");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Reserva confirmada", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: ana@gmail.com\n"));
        assertTrue(messages.get(0).body().contains("Olá Ana!"));
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void testModeDoesNotCreateArchiveCopy() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationHandler(),
                new TestModeHandler(true),
                new ArchiveCopyHandler(true)
        ));
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("ana@gmail.com", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
    }

    @Test
    void blockedMessageDoesNotCreateArchiveCopy() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationHandler(),
                new TestModeHandler(false),
                new ArchiveCopyHandler(false)
        ));
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("invalid@email", "Test", "Body");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void archiveCopyAppearsRightAfterOriginal() {
        MessageProcessor processor = new MessageProcessor(List.of(
                new EmailValidationHandler(),
                new TestModeHandler(false),
                new ArchiveCopyHandler(false)
        ));
        EmailSender sender = new EmailSender(sentMessages, processor);

        sender.send("ana@gmail.com", "First", "Body 1");
        sender.send("bob@gmail.com", "Second", "Body 2");

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        assertEquals("ana@gmail.com", messages.get(0).to());
        assertEquals("First", messages.get(0).subject());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] First", messages.get(1).subject());
        assertEquals("bob@gmail.com", messages.get(2).to());
        assertEquals("Second", messages.get(2).subject());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(3).to());
        assertEquals("[Cópia] Second", messages.get(3).subject());
    }
}
