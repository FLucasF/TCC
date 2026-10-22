package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.EmptyDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailRulesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final SentMessages sentMessages = new SentMessages(database.dataSource());

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @Test
    void validEmailIsSent() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(false));

        sender.send("ana@example.com", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Assunto", messages.get(0).subject());
        assertEquals("Corpo", messages.get(0).body());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void invalidEmailIsBlocked() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(false));

        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("ana@gmail", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void invalidEmailFormatsAreBlocked() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(false));

        sender.send("ana.gmail.com", "Assunto", "Corpo");
        sender.send("", "Assunto", "Corpo");
        sender.send("ana@@example.com", "Assunto", "Corpo");
        sender.send("@example.com", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(4, messages.size());
        for (Message message : messages) {
            assertEquals(MessageStatus.BLOCKED, message.status());
        }
    }

    @Test
    void sentEmailGeneratesArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(false));

        sender.send("ana@example.com", "Assunto teste", "Corpo teste");

        List<Message> messages = sentMessages.all();
        assertEquals(2, messages.size());
        assertEquals("ana@example.com", messages.get(0).to());
        assertEquals("Assunto teste", messages.get(0).subject());
        assertEquals(MessageStatus.SENT, messages.get(0).status());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).to());
        assertEquals("[Cópia] Assunto teste", messages.get(1).subject());
        assertEquals("Corpo teste", messages.get(1).body());
        assertEquals(MessageStatus.SENT, messages.get(1).status());
    }

    @Test
    void blockedEmailDoesNotGenerateArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(false));

        sender.send("invalido", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }

    @Test
    void testModeRedirectsToTestAddress() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(true));

        sender.send("ana@example.com", "Assunto", "Corpo original");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
        assertEquals("[TESTE] Assunto", messages.get(0).subject());
        assertTrue(messages.get(0).body().startsWith("Destinatário original: ana@example.com\n"));
        assertTrue(messages.get(0).body().contains("Corpo original"));
        assertEquals(MessageStatus.SENT, messages.get(0).status());
    }

    @Test
    void testModeDoesNotGenerateArchiveCopy() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(true));

        sender.send("ana@example.com", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).to());
    }

    @Test
    void testModeStillBlocksInvalidEmails() {
        EmailSender sender = new EmailSender(sentMessages, new EmailProcessor(true));

        sender.send("ana@gmail", "Assunto", "Corpo");

        List<Message> messages = sentMessages.all();
        assertEquals(1, messages.size());
        assertEquals("ana@gmail", messages.get(0).to());
        assertEquals(MessageStatus.BLOCKED, messages.get(0).status());
    }
}
